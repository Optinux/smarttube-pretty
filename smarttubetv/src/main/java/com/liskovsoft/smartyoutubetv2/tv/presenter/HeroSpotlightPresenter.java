package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.leanback.widget.RowPresenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.exoplayer2.ui.PlayerView;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e.M3Button;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Renders the immersive Netflix-style autoplaying M3 Expressive Hero Spotlight Stage
 * at the top of the Home feed with muted TextureView preview, thumbnail-to-video crossfade,
 * 10s actual-playback auto-advance, seamless surface scrims, and click-to-resume handoff.
 */
public class HeroSpotlightPresenter extends RowPresenter {
    private WeakReference<HeroViewHolder> mActiveHolder = new WeakReference<>(null);
    private boolean mIsHostResumed = true;
    private boolean mIsRecyclerDragging = false;

    public HeroSpotlightPresenter() {
        setSelectEffectEnabled(false);
    }

    @Override
    protected ViewHolder createRowViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hero_spotlight, parent, false);
        HeroViewHolder vh = new HeroViewHolder(view);
        mActiveHolder = new WeakReference<>(vh);
        return vh;
    }

    @Override
    protected void onBindRowViewHolder(ViewHolder vh, Object item) {
        super.onBindRowViewHolder(vh, item);
        if (vh instanceof HeroViewHolder && item instanceof HeroSpotlightRow) {
            HeroViewHolder heroVh = (HeroViewHolder) vh;
            mActiveHolder = new WeakReference<>(heroVh);
            heroVh.setHostResumed(mIsHostResumed);
            heroVh.setRecyclerDragging(mIsRecyclerDragging);
            heroVh.bind((HeroSpotlightRow) item);
        }
    }

    @Override
    protected void onUnbindRowViewHolder(ViewHolder vh) {
        if (vh instanceof HeroViewHolder) {
            ((HeroViewHolder) vh).unbind();
            if (mActiveHolder.get() == vh) {
                mActiveHolder.clear();
            }
        }
        super.onUnbindRowViewHolder(vh);
    }

    public void onHostResume() {
        mIsHostResumed = true;
        HeroViewHolder vh = mActiveHolder.get();
        if (vh != null) {
            vh.setHostResumed(true);
            vh.updateViewportVisibility();
        }
    }

    public void onHostPause() {
        mIsHostResumed = false;
        HeroViewHolder vh = mActiveHolder.get();
        if (vh != null) {
            vh.setHostResumed(false);
        }
    }

    public void onHostStop() {
        mIsHostResumed = false;
        HeroViewHolder vh = mActiveHolder.get();
        if (vh != null) {
            vh.onHostStop();
        }
    }

    public void onHostDestroy() {
        mIsHostResumed = false;
        HeroViewHolder vh = mActiveHolder.get();
        if (vh != null) {
            vh.onHostDestroy();
        }
        mActiveHolder.clear();
    }

    public void onScrollChanged() {
        HeroViewHolder vh = mActiveHolder.get();
        if (vh != null) {
            vh.updateViewportVisibility();
        }
    }

    public void onScrollDragging(boolean dragging) {
        mIsRecyclerDragging = dragging;
        HeroViewHolder vh = mActiveHolder.get();
        if (vh != null) {
            vh.setRecyclerDragging(dragging);
        }
    }

    private static final class HeroViewHolder extends RowPresenter.ViewHolder
            implements HeroPreviewController.Listener {
        private final FrameLayout mCard;
        private final FrameLayout mMediaContainer;
        private final ImageView mArtwork;
        private final PlayerView mPlayerView;
        private final View mScrimHorizontal;
        private final View mScrimRight;
        private final View mScrimTop;
        private final View mScrimVertical;
        private final LinearLayout mContentColumn;
        private final TextView mBadge;
        private final TextView mCounter;
        private final TextView mTitle;
        private final TextView mSubtitle;
        private final ProgressBar mProgress;
        private final LinearLayout mActionsRow;
        private final M3Button mWatchBtn;
        private final M3Button mOptionsBtn;
        private final M3Button mNextBtn;
        private final LinearLayout mDotsContainer;

        private final HeroPreviewController mPreviewController;
        private HeroSpotlightRow mRow;
        private View mActiveDotProgressFill;
        private int mActiveDotWidthPx;
        private int mLastCardWidth = -1;
        private int mLastCardHeight = -1;
        private boolean mIsTouchInteracting;
        private boolean mIsRecyclerDragging;
        private boolean mScrollListenerAttached;

        private final ViewTreeObserver.OnScrollChangedListener mScrollChangedListener =
                this::updateViewportVisibility;

        @SuppressLint("ClickableViewAccessibility")
        HeroViewHolder(View itemView) {
            super(itemView);
            mCard = itemView.findViewById(R.id.hero_spotlight_card);
            mMediaContainer = itemView.findViewById(R.id.hero_media_container);
            mArtwork = itemView.findViewById(R.id.hero_artwork);
            mPlayerView = itemView.findViewById(R.id.hero_player_view);
            mScrimHorizontal = itemView.findViewById(R.id.hero_scrim_horizontal);
            mScrimRight = itemView.findViewById(R.id.hero_scrim_right);
            mScrimTop = itemView.findViewById(R.id.hero_scrim_top);
            mScrimVertical = itemView.findViewById(R.id.hero_scrim_vertical);
            mContentColumn = itemView.findViewById(R.id.hero_content_column);
            mBadge = itemView.findViewById(R.id.hero_badge);
            mCounter = itemView.findViewById(R.id.hero_counter);
            mTitle = itemView.findViewById(R.id.hero_title);
            mSubtitle = itemView.findViewById(R.id.hero_subtitle);
            mProgress = itemView.findViewById(R.id.hero_progress);
            mActionsRow = itemView.findViewById(R.id.hero_actions_row);
            mWatchBtn = itemView.findViewById(R.id.hero_btn_watch);
            mOptionsBtn = itemView.findViewById(R.id.hero_btn_options);
            mNextBtn = itemView.findViewById(R.id.hero_btn_next);
            mDotsContainer = itemView.findViewById(R.id.hero_dots_container);

            mPreviewController = new HeroPreviewController(itemView.getContext(), mPlayerView, this);

            applyThemeChrome();

            mWatchBtn.setButtonStyle(M3Button.STYLE_FILLED);
            mWatchBtn.setIcon(R.drawable.ic_m3_play_arrow);

            View.OnFocusChangeListener buttonFocusListener = (v, hasFocus) -> {
                boolean insideHero = mWatchBtn.hasFocus() || mOptionsBtn.hasFocus() || mNextBtn.hasFocus();
                if (insideHero && mRow != null && mRow.getListener() != null && mRow.getCurrentVideo() != null) {
                    mRow.getListener().onVideoHighlighted(mRow.getCurrentVideo());
                }
            };
            mWatchBtn.addOnFocusChangeListener(buttonFocusListener);
            mOptionsBtn.addOnFocusChangeListener(buttonFocusListener);
            mNextBtn.addOnFocusChangeListener(buttonFocusListener);

            mOptionsBtn.setButtonStyle(M3Button.STYLE_TONAL);
            mOptionsBtn.setIcon(R.drawable.ic_m3_more_horiz);
            mOptionsBtn.setText(itemView.getContext().getString(R.string.hero_more_options));

            mNextBtn.setButtonStyle(M3Button.STYLE_OUTLINED);
            mNextBtn.setIcon(R.drawable.ic_m3_chevron_right);
            mNextBtn.setText(itemView.getContext().getString(R.string.hero_next_pick));

            mWatchBtn.setOnClickListener(v -> triggerWatch());
            mCard.setOnClickListener(v -> triggerWatch());
            // Keep card non-focusable on D-pad so focus moves directly onto the action buttons
            mCard.setFocusable(false);

            mCard.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        setTouchInteracting(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        setTouchInteracting(false);
                        break;
                }
                return false;
            });

            mOptionsBtn.setOnClickListener(v -> {
                if (mRow != null && mRow.getListener() != null && mRow.getCurrentVideo() != null) {
                    mRow.getListener().onOptionsClicked(mRow.getCurrentVideo());
                }
            });

            mNextBtn.setOnClickListener(v -> advanceSlide(1, true));

            mCard.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                int w = right - left;
                int h = bottom - top;
                adaptResponsiveLayout(w, h);
                updateViewportVisibility();
            });

            mCard.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    attachScrollObserver();
                    updateViewportVisibility();
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    detachScrollObserver();
                    mPreviewController.setHeroVisible(false);
                }
            });
        }

        private void attachScrollObserver() {
            if (!mScrollListenerAttached && mCard.getViewTreeObserver().isAlive()) {
                mCard.getViewTreeObserver().addOnScrollChangedListener(mScrollChangedListener);
                mScrollListenerAttached = true;
            }
        }

        private void detachScrollObserver() {
            if (mScrollListenerAttached && mCard.getViewTreeObserver().isAlive()) {
                mCard.getViewTreeObserver().removeOnScrollChangedListener(mScrollChangedListener);
            }
            mScrollListenerAttached = false;
        }

        private void adaptResponsiveLayout(int cardWidth, int cardHeight) {
            if (cardWidth <= 0 || cardHeight <= 0 || (cardWidth == mLastCardWidth && cardHeight == mLastCardHeight)) {
                return;
            }
            mLastCardWidth = cardWidth;
            mLastCardHeight = cardHeight;

            Context ctx = mCard.getContext();
            float density = ctx.getResources().getDisplayMetrics().density;

            // Keep 16:9 content cleanly center-cropped while adapting to narrow phones vs wide TVs
            int ideal16by9Width = Math.round(cardHeight * (16f / 9f));
            int targetMediaWidth = Math.min(cardWidth, Math.max(ideal16by9Width, Math.round(cardWidth * 0.66f)));
            ViewGroup.LayoutParams mediaLp = mMediaContainer.getLayoutParams();
            if (mediaLp != null && mediaLp.width != targetMediaWidth) {
                mediaLp.width = targetMediaWidth;
                mMediaContainer.setLayoutParams(mediaLp);
            }

            boolean compact = cardWidth < Math.round(520 * density);
            boolean ultraCompact = cardWidth < Math.round(420 * density);

            int padH = Math.round((compact ? 18 : 28) * density);
            mContentColumn.setPaddingRelative(
                    padH, mContentColumn.getPaddingTop(), padH, mContentColumn.getPaddingBottom());

            int maxTextWidth = compact
                    ? Math.max(0, cardWidth - (padH * 2))
                    : Math.min(Math.round(440 * density), Math.round(cardWidth * 0.58f));
            mTitle.setMaxWidth(maxTextWidth);
            mSubtitle.setMaxWidth(maxTextWidth);

            mWatchBtn.setCompact(compact);
            mOptionsBtn.setCompact(compact);
            mNextBtn.setCompact(compact);

            int btnGap = Math.round((compact ? 6 : 10) * density);
            setMarginStart(mOptionsBtn, btnGap);
            setMarginStart(mNextBtn, btnGap);

            mNextBtn.setText(ultraCompact ? null : ctx.getString(R.string.hero_next_pick));
        }

        private static void setMarginStart(View view, int marginStartPx) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                if (mlp.getMarginStart() != marginStartPx) {
                    mlp.setMarginStart(marginStartPx);
                    view.setLayoutParams(mlp);
                }
            }
        }

        private void applyThemeChrome() {
            Context ctx = mCard.getContext();
            int screenBg = ViewUtil.getThemeColor(ctx, R.attr.m3Surface, R.color.m3_grey_surface);
            int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
            int onSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
            // Match neighboring VideoCardPresenter corner treatment (16dp) without a hard hairline border
            float cardRadius = ctx.getResources().getDimension(R.dimen.m3_card_corner_radius);

            mCard.setBackground(ViewUtil.createRoundedDrawable(screenBg, 0, 0, cardRadius));
            ViewUtil.applyRoundedOutline(mCard, cardRadius);

            GradientDrawable horizScrim = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{
                            screenBg,
                            screenBg,
                            ColorUtils.setAlphaComponent(screenBg, 225),
                            ColorUtils.setAlphaComponent(screenBg, 105),
                            Color.TRANSPARENT
                    });
            mScrimHorizontal.setBackground(horizScrim);

            GradientDrawable rightScrim = new GradientDrawable(
                    GradientDrawable.Orientation.RIGHT_LEFT,
                    new int[]{
                            screenBg,
                            ColorUtils.setAlphaComponent(screenBg, 145),
                            Color.TRANSPARENT
                    });
            mScrimRight.setBackground(rightScrim);

            GradientDrawable topScrim = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            ColorUtils.setAlphaComponent(screenBg, 165),
                            Color.TRANSPARENT
                    });
            mScrimTop.setBackground(topScrim);

            GradientDrawable vertScrim = new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[]{
                            screenBg,
                            ColorUtils.setAlphaComponent(screenBg, 215),
                            ColorUtils.setAlphaComponent(screenBg, 85),
                            Color.TRANSPARENT
                    });
            mScrimVertical.setBackground(vertScrim);

            mTitle.setTextColor(onSurface);
            mSubtitle.setTextColor(onSurfaceVariant);
            mCounter.setTextColor(onSurfaceVariant);
            mWatchBtn.refreshThemeColors();
            mOptionsBtn.refreshThemeColors();
            mNextBtn.refreshThemeColors();
        }

        void bind(@NonNull HeroSpotlightRow row) {
            mRow = row;
            if (ViewCompat.isAttachedToWindow(mCard)) {
                attachScrollObserver();
            }
            renderCurrentSlide(false);
            updateViewportVisibility();
        }

        void unbind() {
            detachScrollObserver();
            mPreviewController.release();
            Context ctx = mCard.getContext().getApplicationContext();
            Glide.with(ctx).clear(mArtwork);
            mRow = null;
        }

        void setHostResumed(boolean resumed) {
            mPreviewController.setResumed(resumed);
        }

        void onHostStop() {
            mPreviewController.release();
        }

        void onHostDestroy() {
            detachScrollObserver();
            mPreviewController.release();
        }

        void setRecyclerDragging(boolean dragging) {
            mIsRecyclerDragging = dragging;
            mPreviewController.setUserInteracting(mIsTouchInteracting || mIsRecyclerDragging);
        }

        private void setTouchInteracting(boolean interacting) {
            mIsTouchInteracting = interacting;
            mPreviewController.setUserInteracting(mIsTouchInteracting || mIsRecyclerDragging);
        }

        void updateViewportVisibility() {
            boolean visible = isHeroVisibleInViewport();
            mPreviewController.setHeroVisible(visible);
        }

        private boolean isHeroVisibleInViewport() {
            if (!ViewCompat.isAttachedToWindow(mCard) || mCard.getVisibility() != View.VISIBLE || mCard.getHeight() <= 0) {
                return false;
            }
            Rect visibleRect = new Rect();
            if (!mCard.getGlobalVisibleRect(visibleRect)) {
                return false;
            }
            return visibleRect.height() >= mCard.getHeight() * 0.25f;
        }

        private void triggerWatch() {
            if (mRow != null && mRow.getListener() != null && mRow.getCurrentVideo() != null) {
                long previewPosMs = mPreviewController.getCurrentPositionMs();
                mPreviewController.setResumed(false);
                mRow.getListener().onWatchClicked(mRow.getCurrentVideo(), previewPosMs);
            }
        }

        @Override
        public void onAutoAdvance() {
            if (mRow != null && mRow.getVideos().size() > 1 && ViewCompat.isAttachedToWindow(mCard)) {
                advanceSlide(1, true);
            }
        }

        @Override
        public void onPlaybackProgress(float fraction) {
            if (mActiveDotProgressFill == null || mActiveDotWidthPx <= 0) {
                return;
            }
            float clamped = Math.max(0f, Math.min(1f, fraction));
            int targetWidth = Math.round(mActiveDotWidthPx * clamped);
            ViewGroup.LayoutParams lp = mActiveDotProgressFill.getLayoutParams();
            if (lp != null && lp.width != targetWidth) {
                lp.width = targetWidth;
                mActiveDotProgressFill.setLayoutParams(lp);
            }
        }

        private void advanceSlide(int delta, boolean animate) {
            if (mRow == null || mRow.getVideos().isEmpty()) {
                return;
            }
            mRow.setSelectedIndex(mRow.getSelectedIndex() + delta);
            renderCurrentSlide(animate);
            if (mRow.getListener() != null && mRow.getCurrentVideo() != null) {
                mRow.getListener().onVideoHighlighted(mRow.getCurrentVideo());
            }
        }

        private void renderCurrentSlide(boolean animate) {
            if (mRow == null) {
                return;
            }
            List<Video> videos = mRow.getVideos();
            Video video = mRow.getCurrentVideo();
            if (video == null) {
                return;
            }
            Context ctx = mCard.getContext();

            // Badge styling
            String badgeText;
            int badgeBg;
            int badgeFg;
            int primaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
            int onPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);
            int secondaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container);
            int onSecondaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnSecondaryContainer, R.color.m3_grey_on_secondary_container);

            if (video.isLive) {
                badgeText = ctx.getString(R.string.hero_live_now).toUpperCase();
                badgeBg = ContextCompat.getColor(ctx, R.color.m3_badge_live_bg);
                badgeFg = ContextCompat.getColor(ctx, R.color.m3_badge_text);
            } else if (video.isUpcoming) {
                badgeText = ctx.getString(R.string.hero_upcoming).toUpperCase();
                badgeBg = ContextCompat.getColor(ctx, R.color.m3_badge_live_bg);
                badgeFg = ContextCompat.getColor(ctx, R.color.m3_badge_text);
            } else if (video.percentWatched > 4f) {
                badgeText = ctx.getString(R.string.hero_resume).toUpperCase() + "  •  " + Math.round(video.percentWatched) + "%";
                badgeBg = primaryContainer;
                badgeFg = onPrimaryContainer;
            } else {
                String section = !TextUtils.isEmpty(mRow.getSectionLabel())
                        ? mRow.getSectionLabel()
                        : ctx.getString(R.string.hero_featured);
                badgeText = section.toUpperCase();
                badgeBg = secondaryContainer;
                badgeFg = onSecondaryContainer;
            }

            float pillRadius = ctx.getResources().getDimension(R.dimen.m3_pill_corner_radius);
            mBadge.setText(badgeText);
            mBadge.setTextColor(badgeFg);
            mBadge.setBackground(ViewUtil.createRoundedDrawable(badgeBg, 0, 0, pillRadius));

            if (videos.size() > 1) {
                mCounter.setVisibility(View.VISIBLE);
                mCounter.setText((mRow.getSelectedIndex() + 1) + " / " + videos.size());
                mNextBtn.setVisibility(View.VISIBLE);
            } else {
                mCounter.setVisibility(View.GONE);
                mNextBtn.setVisibility(View.GONE);
            }

            mTitle.setText(video.getTitleFull() != null ? video.getTitleFull() : video.getTitle());
            CharSequence subtitle = video.getSecondTitleFull() != null ? video.getSecondTitleFull() : video.getSecondTitle();
            if (TextUtils.isEmpty(subtitle)) {
                subtitle = video.getAuthor();
            }
            mSubtitle.setText(subtitle);
            mSubtitle.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);

            if (video.percentWatched > 0f) {
                int progressVal = video.percentWatched < 1f ? 1 : Math.round(video.percentWatched);
                mProgress.setProgress(progressVal);
                mProgress.setVisibility(View.VISIBLE);
            } else {
                mProgress.setVisibility(View.GONE);
            }

            mWatchBtn.setText(video.percentWatched > 4f
                    ? ctx.getString(R.string.hero_resume)
                    : ctx.getString(R.string.hero_watch_now));

            if (animate) {
                float density = ctx.getResources().getDisplayMetrics().density;
                M3Motion.animateEnter(mTitle, 10f * density, 0f);
                M3Motion.animateEnter(mSubtitle, 10f * density, 0f);
            }

            // Load thumbnail immediately (crossfades to video once first frame renders)
            if (!(ctx instanceof Activity) || (Build.VERSION.SDK_INT < 17 || !((Activity) ctx).isDestroyed())) {
                int thumbQuality = MainUIData.instance(ctx).getThumbQuality();
                String primaryUrl = !TextUtils.isEmpty(video.bgImageUrl)
                        ? video.bgImageUrl
                        : ClickbaitRemover.updateThumbnail(video, thumbQuality);
                Glide.with(ctx)
                        .load(primaryUrl)
                        .apply(ViewUtil.glideOptions().centerCrop())
                        .diskCacheStrategy(Build.VERSION.SDK_INT > 21 ? DiskCacheStrategy.AUTOMATIC : DiskCacheStrategy.NONE)
                        .error(Glide.with(ctx)
                                .load(video.getCardImageUrl())
                                .apply(ViewUtil.glideOptions().centerCrop())
                                .error(R.drawable.card_placeholder))
                        .into(mArtwork);
            }

            renderDots(videos.size(), mRow.getSelectedIndex());

            Video nextVideo = null;
            if (videos.size() > 1) {
                int nextIdx = (mRow.getSelectedIndex() + 1) % videos.size();
                nextVideo = videos.get(nextIdx);
            }
            mPreviewController.bindVideo(video, nextVideo);
        }

        private void renderDots(int count, int activeIndex) {
            mDotsContainer.removeAllViews();
            mActiveDotProgressFill = null;
            mActiveDotWidthPx = 0;
            if (count <= 1) {
                return;
            }
            Context ctx = mDotsContainer.getContext();
            float density = ctx.getResources().getDisplayMetrics().density;
            int activeColor = ViewUtil.getThemeColor(ctx, R.attr.m3Primary, R.color.m3_grey_primary);
            int inactiveColor = ColorUtils.setAlphaComponent(
                    ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant), 110);
            int activeTrackColor = ColorUtils.setAlphaComponent(activeColor, 90);
            float pillRadius = ctx.getResources().getDimension(R.dimen.m3_pill_corner_radius);

            for (int i = 0; i < count; i++) {
                final int idx = i;
                boolean active = i == activeIndex;
                int w = Math.round((active ? 26 : 7) * density);
                int h = Math.round(7 * density);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
                if (i > 0) {
                    lp.setMarginStart(Math.round(6 * density));
                }

                View dotView;
                if (active) {
                    FrameLayout pillTrack = new FrameLayout(ctx);
                    pillTrack.setBackground(ViewUtil.createRoundedDrawable(activeTrackColor, 0, 0, pillRadius));
                    ViewUtil.applyRoundedOutline(pillTrack, pillRadius);

                    View fill = new View(ctx);
                    fill.setBackground(ViewUtil.createRoundedDrawable(activeColor, 0, 0, pillRadius));
                    pillTrack.addView(fill, new FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START));

                    mActiveDotProgressFill = fill;
                    mActiveDotWidthPx = w;
                    dotView = pillTrack;
                } else {
                    View dot = new View(ctx);
                    dot.setBackground(ViewUtil.createRoundedDrawable(inactiveColor, 0, 0, pillRadius));
                    dotView = dot;
                }

                dotView.setOnClickListener(v -> {
                    if (mRow != null) {
                        mRow.setSelectedIndex(idx);
                        renderCurrentSlide(true);
                        if (mRow.getListener() != null && mRow.getCurrentVideo() != null) {
                            mRow.getListener().onVideoHighlighted(mRow.getCurrentVideo());
                        }
                    }
                });
                mDotsContainer.addView(dotView, lp);
            }
        }
    }
}
