package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.video.VideoListener;
import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.ServiceManager;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.ExoMediaSourceFactory;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.renderer.CustomOverridesRenderersFactory;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import com.liskovsoft.youtubeapi.service.internal.MediaServiceData;

import java.io.File;

import io.reactivex.disposables.Disposable;

/**
 * Manages the single reused muted ExoPlayer preview instance for the Home hero spotlight.
 * Handles Data Saver / Battery Saver / metered guards, first-frame crossfades, error/stall
 * fallback to thumbnail, 10s actual-playback auto-advance timing, and next-item preloading.
 */
final class HeroPreviewController implements Player.EventListener, VideoListener {
    interface Listener {
        void onAutoAdvance();
        void onPlaybackProgress(float fraction);
    }

    private static final String TAG = HeroPreviewController.class.getSimpleName();
    static final long AUTO_ADVANCE_DURATION_MS = 10_000L;
    private static final long CROSSFADE_DURATION_MS = 350L;
    private static final long LOAD_TIMEOUT_MS = 8_000L;
    private static final long BUFFER_STALL_TIMEOUT_MS = 1_200L;
    private static final long TIMER_TICK_MS = 100L;
    private static final long FORMAT_CACHE_TTL_MS = 10 * 60_000L;
    private static final long PREVIEW_CACHE_MAX_BYTES = 32L * 1024L * 1024L;
    private static final int MAX_PREVIEW_WIDTH = 1280;
    private static final int MAX_PREVIEW_HEIGHT = 720;
    private static final int MAX_PREVIEW_BITRATE = 2_500_000;

    private static SimpleCache sSharedCache;

    private static final class CachedFormat {
        final MediaItemFormatInfo formatInfo;
        final long timestampMs;

        CachedFormat(MediaItemFormatInfo formatInfo) {
            this.formatInfo = formatInfo;
            this.timestampMs = SystemClock.elapsedRealtime();
        }

        boolean isFresh() {
            return SystemClock.elapsedRealtime() - timestampMs < FORMAT_CACHE_TTL_MS;
        }
    }

    private final Context mAppContext;
    private final PlayerView mPlayerView;
    private final Listener mListener;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ExoMediaSourceFactory mMediaSourceFactory;
    private final LruCache<String, CachedFormat> mFormatCache = new LruCache<>(8);

    private SimpleExoPlayer mPlayer;
    private DefaultTrackSelector mTrackSelector;
    private Disposable mFormatInfoDisposable;
    private Disposable mPreloadDisposable;

    private Video mCurrentVideo;
    private Video mNextVideo;
    private String mActiveVideoId;

    private boolean mIsResumed = true;
    private boolean mIsHeroVisible = true;
    private boolean mIsUserInteracting = false;
    private boolean mHasRenderedFirstFrame = false;
    private boolean mAbortedToThumbnail = false;

    private long mAccumulatedPlayMs = 0L;
    private long mLastTickElapsedMs = 0L;
    private boolean mTickerRunning = false;

    private final Runnable mLoadTimeoutTask = this::onLoadTimedOut;
    private final Runnable mBufferStallTask = this::onBufferingStalled;
    private final Runnable mPlaybackTickTask = new Runnable() {
        @Override
        public void run() {
            if (!mTickerRunning) {
                return;
            }
            if (!canAccumulatePlaybackTime()) {
                mTickerRunning = false;
                return;
            }
            long now = SystemClock.elapsedRealtime();
            if (mLastTickElapsedMs > 0L) {
                mAccumulatedPlayMs += Math.max(0L, now - mLastTickElapsedMs);
            }
            mLastTickElapsedMs = now;

            float fraction = Math.min(1f, (float) mAccumulatedPlayMs / AUTO_ADVANCE_DURATION_MS);
            if (mListener != null) {
                mListener.onPlaybackProgress(fraction);
            }
            if (mAccumulatedPlayMs >= AUTO_ADVANCE_DURATION_MS) {
                mTickerRunning = false;
                if (mListener != null) {
                    mListener.onAutoAdvance();
                }
                return;
            }
            mHandler.postDelayed(this, TIMER_TICK_MS);
        }
    };

    HeroPreviewController(@NonNull Context context, @NonNull PlayerView playerView, @NonNull Listener listener) {
        mAppContext = context.getApplicationContext();
        mPlayerView = playerView;
        mListener = listener;
        mMediaSourceFactory = new ExoMediaSourceFactory(mAppContext, getOrCreateCache(mAppContext));
    }

    @Nullable
    private static synchronized SimpleCache getOrCreateCache(Context context) {
        if (sSharedCache == null) {
            try {
                File cacheDir = new File(context.getCacheDir(), "hero_preview_cache");
                sSharedCache = new SimpleCache(
                        cacheDir,
                        new LeastRecentlyUsedCacheEvictor(PREVIEW_CACHE_MAX_BYTES),
                        new ExoDatabaseProvider(context));
            } catch (Throwable t) {
                Log.w(TAG, "Unable to initialize hero preview SimpleCache: " + t.getMessage());
                sSharedCache = null;
            }
        }
        return sSharedCache;
    }

    void bindVideo(@Nullable Video video, @Nullable Video nextVideo) {
        mCurrentVideo = video;
        mNextVideo = nextVideo;
        mActiveVideoId = video != null ? video.videoId : null;
        mAbortedToThumbnail = false;

        resetToThumbnailImmediate();
        resetPlaybackTimer();
        cancelPreloadWork();

        if (!canStartVideoPreview(video)) {
            stopPlayerKeepThumbnail();
            return;
        }

        startLoadingCurrentVideo(video);
    }

    void setResumed(boolean resumed) {
        if (mIsResumed == resumed) {
            return;
        }
        mIsResumed = resumed;
        if (!resumed) {
            pausePlaybackAndTimer();
        } else {
            resumeIfNeeded();
        }
    }

    void setHeroVisible(boolean visible) {
        if (mIsHeroVisible == visible) {
            return;
        }
        mIsHeroVisible = visible;
        if (!visible) {
            pausePlaybackAndTimer();
        } else {
            resumeIfNeeded();
        }
    }

    void setUserInteracting(boolean interacting) {
        if (mIsUserInteracting == interacting) {
            return;
        }
        mIsUserInteracting = interacting;
        if (interacting) {
            stopPlaybackTicker();
        } else if (canAccumulatePlaybackTime()) {
            startPlaybackTicker();
        }
    }

    void resetPlaybackTimer() {
        mAccumulatedPlayMs = 0L;
        mLastTickElapsedMs = 0L;
        stopPlaybackTicker();
        if (mListener != null) {
            mListener.onPlaybackProgress(0f);
        }
        if (canAccumulatePlaybackTime()) {
            startPlaybackTicker();
        }
    }

    /**
     * Returns the exact preview playback position in milliseconds if video playback has rendered,
     * or -1L when staying on thumbnail (e.g., Data Saver, load error, or before first frame).
     */
    long getCurrentPositionMs() {
        if (mPlayer == null || !mHasRenderedFirstFrame || mAbortedToThumbnail) {
            return -1L;
        }
        return Math.max(0L, mPlayer.getCurrentPosition());
    }

    void release() {
        cancelTimeouts();
        stopPlaybackTicker();
        disposeFormatRequest();
        cancelPreloadWork();

        if (mPlayer != null) {
            try {
                mPlayer.removeListener(this);
                mPlayer.removeVideoListener(this);
                mPlayer.stop(true);
                mPlayer.clearVideoSurface();
                mPlayer.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing hero preview player: " + e.getMessage());
            }
            mPlayer = null;
            mTrackSelector = null;
        }

        mPlayerView.setPlayer(null);
        resetToThumbnailImmediate();
    }

    private void resumeIfNeeded() {
        if (!mIsResumed || !mIsHeroVisible || mCurrentVideo == null) {
            return;
        }
        if (!canStartVideoPreview(mCurrentVideo)) {
            stopPlayerKeepThumbnail();
            return;
        }
        if (mPlayer == null) {
            // Re-initialize preview at the same item when returning home after onStop/release
            mAbortedToThumbnail = false;
            resetToThumbnailImmediate();
            startLoadingCurrentVideo(mCurrentVideo);
            return;
        }
        if (mAbortedToThumbnail) {
            return;
        }
        if (mPlayer.getPlaybackState() == Player.STATE_IDLE && mActiveVideoId != null) {
            startLoadingCurrentVideo(mCurrentVideo);
            return;
        }
        mPlayer.setPlayWhenReady(true);
        if (mHasRenderedFirstFrame && mPlayer.getPlaybackState() == Player.STATE_READY) {
            crossfadeToVideo();
        }
        if (canAccumulatePlaybackTime()) {
            startPlaybackTicker();
        }
    }

    private void pausePlaybackAndTimer() {
        cancelTimeouts();
        stopPlaybackTicker();
        disposeFormatRequest();
        if (mPlayer != null) {
            mPlayer.setPlayWhenReady(false);
        }
    }

    private void startLoadingCurrentVideo(@NonNull Video video) {
        disposeFormatRequest();
        cancelTimeouts();

        final String targetVideoId = video.videoId;
        if (TextUtils.isEmpty(targetVideoId)) {
            return;
        }

        mHandler.postDelayed(mLoadTimeoutTask, LOAD_TIMEOUT_MS);

        CachedFormat cached = mFormatCache.get(targetVideoId);
        if (cached != null && cached.isFresh()) {
            prepareFromFormatInfo(targetVideoId, cached.formatInfo);
            return;
        }

        ServiceManager service = YouTubeServiceManager.instance();
        service.allowRestrictedVideos(!GeneralData.instance(mAppContext).isChildModeEnabled());
        MediaItemService mediaItemService = service.getMediaItemService();
        mFormatInfoDisposable = mediaItemService.getFormatInfoObserve(targetVideoId)
                .subscribe(formatInfo -> {
                    if (!TextUtils.equals(targetVideoId, mActiveVideoId)) {
                        return;
                    }
                    if (formatInfo != null && !formatInfo.isUnplayable()) {
                        mFormatCache.put(targetVideoId, new CachedFormat(formatInfo));
                    }
                    prepareFromFormatInfo(targetVideoId, formatInfo);
                }, error -> {
                    if (TextUtils.equals(targetVideoId, mActiveVideoId)) {
                        Log.w(TAG, "Hero preview format load failed: " + error.getMessage());
                        fallbackToThumbnail();
                    }
                });
    }

    private void prepareFromFormatInfo(@NonNull String videoId, @Nullable MediaItemFormatInfo formatInfo) {
        if (!TextUtils.equals(videoId, mActiveVideoId) || !mIsResumed || !mIsHeroVisible) {
            return;
        }
        if (formatInfo == null || formatInfo.isUnplayable() || !formatInfo.containsMedia()) {
            fallbackToThumbnail();
            return;
        }

        MediaSource mediaSource = buildMediaSource(formatInfo);
        if (mediaSource == null) {
            fallbackToThumbnail();
            return;
        }

        SimpleExoPlayer player = ensurePlayer();
        mHasRenderedFirstFrame = false;
        mAbortedToThumbnail = false;
        player.setVolume(0f);
        player.setPlayWhenReady(mIsResumed && mIsHeroVisible);
        player.prepare(mediaSource, true, true);
    }

    @Nullable
    private MediaSource buildMediaSource(@NonNull MediaItemFormatInfo formatInfo) {
        try {
            if (acceptAdaptiveFormats(formatInfo) && formatInfo.containsDashFormats() && !preferSabrLive(formatInfo)) {
                return mMediaSourceFactory.fromDashFormatInfo(formatInfo);
            } else if (acceptAdaptiveFormats(formatInfo) && formatInfo.containsSabrFormats()) {
                return mMediaSourceFactory.fromSabrFormatInfo(formatInfo);
            } else if (acceptDashLive(formatInfo)) {
                return mMediaSourceFactory.fromDashManifestUrl(formatInfo.getDashManifestUrl());
            } else if (formatInfo.isLive() && formatInfo.containsHlsUrl()) {
                return mMediaSourceFactory.fromHlsPlaylist(formatInfo.getHlsManifestUrl());
            } else if (formatInfo.containsUrlFormats()) {
                return mMediaSourceFactory.fromUrlList(formatInfo.createUrlList());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to build hero MediaSource: " + e.getMessage());
        }
        return null;
    }

    private SimpleExoPlayer ensurePlayer() {
        if (mPlayer != null) {
            if (mPlayerView.getPlayer() != mPlayer) {
                mPlayerView.setPlayer(mPlayer);
            }
            return mPlayer;
        }

        mTrackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());
        mTrackSelector.setParameters(mTrackSelector.buildUponParameters()
                .setMaxVideoSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
                .setMaxVideoBitrate(MAX_PREVIEW_BITRATE));
        DefaultRenderersFactory renderersFactory = new CustomOverridesRenderersFactory(mAppContext);
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(5_000, 15_000, 1_000, 2_000)
                .createDefaultLoadControl();

        mPlayer = ExoPlayerFactory.newSimpleInstance(mAppContext, renderersFactory, mTrackSelector, loadControl);

        // Configure muted preview: zero volume, no audio focus requested, and disable audio/subtitle tracks
        DefaultTrackSelector.ParametersBuilder params = mTrackSelector.buildUponParameters();
        for (int i = 0; i < mPlayer.getRendererCount(); i++) {
            int type = mPlayer.getRendererType(i);
            if (type == C.TRACK_TYPE_AUDIO || type == C.TRACK_TYPE_TEXT) {
                params.setRendererDisabled(i, true);
            }
        }
        mTrackSelector.setParameters(params);

        mPlayer.setVolume(0f);
        mPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        mPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC);
        mPlayer.addListener(this);
        mPlayer.addVideoListener(this);
        mPlayerView.setPlayer(mPlayer);
        return mPlayer;
    }

    @Override
    public void onRenderedFirstFrame() {
        if (mAbortedToThumbnail || !mIsResumed || !mIsHeroVisible) {
            return;
        }
        mHandler.removeCallbacks(mLoadTimeoutTask);
        mHandler.removeCallbacks(mBufferStallTask);
        mHasRenderedFirstFrame = true;
        crossfadeToVideo();
        if (canAccumulatePlaybackTime()) {
            startPlaybackTicker();
        }
        preloadNextVideo();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (mAbortedToThumbnail) {
            return;
        }
        if (playbackState == Player.STATE_BUFFERING) {
            stopPlaybackTicker();
            if (mHasRenderedFirstFrame) {
                mHandler.removeCallbacks(mBufferStallTask);
                mHandler.postDelayed(mBufferStallTask, BUFFER_STALL_TIMEOUT_MS);
            }
        } else if (playbackState == Player.STATE_READY) {
            mHandler.removeCallbacks(mBufferStallTask);
            if (playWhenReady && mHasRenderedFirstFrame && mIsResumed && mIsHeroVisible) {
                crossfadeToVideo();
                if (canAccumulatePlaybackTime()) {
                    startPlaybackTicker();
                }
            } else if (!playWhenReady) {
                stopPlaybackTicker();
            }
        } else if (playbackState == Player.STATE_ENDED) {
            cancelTimeouts();
            stopPlaybackTicker();
            if (playWhenReady && mHasRenderedFirstFrame && mListener != null) {
                // Videos shorter than 10s advance to the next item at the end instead of looping
                mListener.onAutoAdvance();
            }
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.w(TAG, "Hero preview playback error: " + error.getMessage());
        fallbackToThumbnail();
    }

    private void onLoadTimedOut() {
        if (!mHasRenderedFirstFrame) {
            Log.d(TAG, "Hero preview load timed out; staying on thumbnail.");
            fallbackToThumbnail();
        }
    }

    private void onBufferingStalled() {
        if (mPlayer != null && mPlayer.getPlaybackState() == Player.STATE_BUFFERING) {
            Log.d(TAG, "Hero preview buffering stalled; showing thumbnail.");
            crossfadeToThumbnail();
        }
    }

    private void fallbackToThumbnail() {
        mAbortedToThumbnail = true;
        stopPlayerKeepThumbnail();
    }

    private void stopPlayerKeepThumbnail() {
        cancelTimeouts();
        stopPlaybackTicker();
        disposeFormatRequest();
        mHasRenderedFirstFrame = false;
        if (mPlayer != null) {
            try {
                mPlayer.stop(true);
            } catch (Exception ignored) {
            }
        }
        crossfadeToThumbnail();
    }

    private void resetToThumbnailImmediate() {
        mHasRenderedFirstFrame = false;
        mPlayerView.animate().cancel();
        mPlayerView.setAlpha(0f);
    }

    private void crossfadeToVideo() {
        mPlayerView.animate().cancel();
        if (mPlayerView.getAlpha() >= 0.99f) {
            mPlayerView.setAlpha(1f);
            return;
        }
        if (!M3Motion.areAnimationsEnabled(mPlayerView.getContext())) {
            mPlayerView.setAlpha(1f);
            return;
        }
        mPlayerView.animate()
                .alpha(1f)
                .setDuration(CROSSFADE_DURATION_MS)
                .setInterpolator(M3Motion.defaultEffects())
                .start();
    }

    private void crossfadeToThumbnail() {
        mPlayerView.animate().cancel();
        if (mPlayerView.getAlpha() <= 0.01f) {
            mPlayerView.setAlpha(0f);
            return;
        }
        if (!M3Motion.areAnimationsEnabled(mPlayerView.getContext())) {
            mPlayerView.setAlpha(0f);
            return;
        }
        mPlayerView.animate()
                .alpha(0f)
                .setDuration(CROSSFADE_DURATION_MS)
                .setInterpolator(M3Motion.fastEffects())
                .start();
    }

    private boolean canAccumulatePlaybackTime() {
        return mHasRenderedFirstFrame
                && !mAbortedToThumbnail
                && mIsResumed
                && mIsHeroVisible
                && !mIsUserInteracting
                && mPlayer != null
                && mPlayer.getPlayWhenReady()
                && mPlayer.getPlaybackState() == Player.STATE_READY;
    }

    private void startPlaybackTicker() {
        if (mTickerRunning) {
            return;
        }
        mTickerRunning = true;
        mLastTickElapsedMs = SystemClock.elapsedRealtime();
        mHandler.removeCallbacks(mPlaybackTickTask);
        mHandler.postDelayed(mPlaybackTickTask, TIMER_TICK_MS);
    }

    private void stopPlaybackTicker() {
        mTickerRunning = false;
        mLastTickElapsedMs = 0L;
        mHandler.removeCallbacks(mPlaybackTickTask);
    }

    private void cancelTimeouts() {
        mHandler.removeCallbacks(mLoadTimeoutTask);
        mHandler.removeCallbacks(mBufferStallTask);
    }

    private void disposeFormatRequest() {
        RxHelper.disposeActions(mFormatInfoDisposable);
        mFormatInfoDisposable = null;
    }

    private void cancelPreloadWork() {
        RxHelper.disposeActions(mPreloadDisposable);
        mPreloadDisposable = null;
    }

    private void preloadNextVideo() {
        if (mNextVideo == null || mCurrentVideo == null || mNextVideo == mCurrentVideo) {
            return;
        }
        if (TextUtils.equals(mNextVideo.videoId, mCurrentVideo.videoId)) {
            return;
        }

        // Preload next thumbnail in Glide
        int thumbQuality = MainUIData.instance(mAppContext).getThumbQuality();
        String nextThumb = !TextUtils.isEmpty(mNextVideo.bgImageUrl)
                ? mNextVideo.bgImageUrl
                : ClickbaitRemover.updateThumbnail(mNextVideo, thumbQuality);
        if (!TextUtils.isEmpty(nextThumb)) {
            Glide.with(mAppContext).load(nextThumb).preload();
        }

        if (!canStartVideoPreview(mNextVideo)) {
            return;
        }
        final String nextId = mNextVideo.videoId;
        CachedFormat existing = mFormatCache.get(nextId);
        if (existing != null && existing.isFresh()) {
            return;
        }

        RxHelper.disposeActions(mPreloadDisposable);
        mPreloadDisposable = YouTubeServiceManager.instance()
                .getMediaItemService()
                .getFormatInfoObserve(nextId)
                .subscribe(formatInfo -> {
                    if (formatInfo != null && !formatInfo.isUnplayable()) {
                        mFormatCache.put(nextId, new CachedFormat(formatInfo));
                    }
                }, error -> {
                    // Best-effort preload; ignore errors silently
                });
    }

    private boolean canStartVideoPreview(@Nullable Video video) {
        if (video == null || !video.hasVideo() || TextUtils.isEmpty(video.videoId) || video.isUpcoming) {
            return false;
        }
        return !shouldSkipAutoplay(mAppContext);
    }

    static boolean shouldSkipAutoplay(@NonNull Context context) {
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null && pm.isPowerSaveMode()) {
                    return true;
                }
            }
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= 24
                        && cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) {
                    return true;
                }
                if (Build.VERSION.SDK_INT >= 16 && cm.isActiveNetworkMetered()) {
                    return true;
                }
            }
        } catch (SecurityException | RuntimeException ignored) {
        }
        return false;
    }

    private boolean acceptAdaptiveFormats(@NonNull MediaItemFormatInfo formatInfo) {
        PlayerData playerData = PlayerData.instance(mAppContext);
        PlayerTweaksData tweaks = PlayerTweaksData.instance(mAppContext);
        if (playerData.isLegacyCodecsForced() && formatInfo.containsUrlFormats()) {
            return false;
        }
        if (tweaks.isHlsStreamsForced() && formatInfo.isLive() && formatInfo.containsHlsUrl()) {
            return false;
        }
        if (formatInfo.isLive() && formatInfo.getStartTimeMs() == 0) {
            return false;
        }
        if (formatInfo.isLive() && tweaks.isDashUrlStreamsForced() && formatInfo.containsDashUrl()) {
            return false;
        }
        return true;
    }

    private boolean preferSabrLive(@NonNull MediaItemFormatInfo formatInfo) {
        return MediaServiceData.instance().isSabrLiveEnabled()
                && formatInfo.isLive()
                && formatInfo.containsSabrFormats();
    }

    private boolean acceptDashLive(@NonNull MediaItemFormatInfo formatInfo) {
        PlayerTweaksData tweaks = PlayerTweaksData.instance(mAppContext);
        if (tweaks.isHlsStreamsForced() && formatInfo.isLive() && formatInfo.containsHlsUrl()) {
            return false;
        }
        return formatInfo.isLive() && formatInfo.containsDashUrl();
    }
}
