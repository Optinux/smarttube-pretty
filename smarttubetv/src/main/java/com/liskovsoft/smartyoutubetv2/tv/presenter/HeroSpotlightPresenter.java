package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.leanback.widget.RowPresenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e.M3Button;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.List;

/**
 * Renders the immersive M3 Expressive Hero Spotlight Stage at the top of browse row feeds.
 * Inspired by Apple TV / Google TV / Plex front-ends with edge-blended artwork, rich metadata,
 * M3 shape-morphing action buttons, and gentle idle auto-rotation.
 */
public class HeroSpotlightPresenter extends RowPresenter {
    private static final long AUTO_ROTATE_INTERVAL_MS = 9_000L;

    public HeroSpotlightPresenter() {
        setSelectEffectEnabled(false);
    }

    @Override
    protected ViewHolder createRowViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hero_spotlight, parent, false);
        return new HeroViewHolder(view);
    }

    @Override
    protected void onBindRowViewHolder(ViewHolder vh, Object item) {
        super.onBindRowViewHolder(vh, item);
        if (vh instanceof HeroViewHolder && item instanceof HeroSpotlightRow) {
            ((HeroViewHolder) vh).bind((HeroSpotlightRow) item);
        }
    }

    @Override
    protected void onUnbindRowViewHolder(ViewHolder vh) {
        if (vh instanceof HeroViewHolder) {
            ((HeroViewHolder) vh).unbind();
        }
        super.onUnbindRowViewHolder(vh);
    }

    private static final class HeroViewHolder extends RowPresenter.ViewHolder {
        private final FrameLayout mCard;
        private final ImageView mArtwork;
        private final View mScrimHorizontal;
        private final View mScrimVertical;
        private final TextView mBadge;
        private final TextView mCounter;
        private final TextView mTitle;
        private final TextView mSubtitle;
        private final ProgressBar mProgress;
        private final M3Button mWatchBtn;
        private final M3Button mOptionsBtn;
        private final M3Button mNextBtn;
        private final LinearLayout mDotsContainer;
        private final Handler mHandler = new Handler(Looper.getMainLooper());
        private HeroSpotlightRow mRow;
        private boolean mAnyButtonFocused;

        private final Runnable mAutoRotateTask = new Runnable() {
            @Override
            public void run() {
                if (mRow == null || mRow.getVideos().size() <= 1 || mAnyButtonFocused) {
                    return;
                }
                if (!mCard.isAttachedToWindow()) {
                    return;
                }
                advanceSlide(1, true);
                mHandler.postDelayed(this, AUTO_ROTATE_INTERVAL_MS);
            }
        };

        HeroViewHolder(View itemView) {
            super(itemView);
            mCard = itemView.findViewById(R.id.hero_spotlight_card);
            mArtwork = itemView.findViewById(R.id.hero_artwork);
            mScrimHorizontal = itemView.findViewById(R.id.hero_scrim_horizontal);
            mScrimVertical = itemView.findViewById(R.id.hero_scrim_vertical);
            mBadge = itemView.findViewById(R.id.hero_badge);
            mCounter = itemView.findViewById(R.id.hero_counter);
            mTitle = itemView.findViewById(R.id.hero_title);
            mSubtitle = itemView.findViewById(R.id.hero_subtitle);
            mProgress = itemView.findViewById(R.id.hero_progress);
            mWatchBtn = itemView.findViewById(R.id.hero_btn_watch);
            mOptionsBtn = itemView.findViewById(R.id.hero_btn_options);
            mNextBtn = itemView.findViewById(R.id.hero_btn_next);
            mDotsContainer = itemView.findViewById(R.id.hero_dots_container);

            applyThemeChrome();

            mWatchBtn.setButtonStyle(M3Button.STYLE_FILLED);
            mWatchBtn.setIcon(R.drawable.ic_m3_play_arrow);

            View.OnFocusChangeListener buttonFocusListener = (v, hasFocus) -> {
                boolean wasInside = mAnyButtonFocused;
                boolean insideHero = mWatchBtn.hasFocus() || mOptionsBtn.hasFocus() || mNextBtn.hasFocus();
                mAnyButtonFocused = insideHero;
                if (insideHero) {
                    stopAutoRotate();
                    if (!wasInside && mRow != null && mRow.getListener() != null && mRow.getCurrentVideo() != null) {
                        mRow.getListener().onVideoHighlighted(mRow.getCurrentVideo());
                    }
                } else {
                    scheduleAutoRotate();
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

            mWatchBtn.setOnClickListener(v -> {
                if (mRow != null && mRow.getListener() != null && mRow.getCurrentVideo() != null) {
                    mRow.getListener().onWatchClicked(mRow.getCurrentVideo());
                }
            });

            mOptionsBtn.setOnClickListener(v -> {
                if (mRow != null && mRow.getListener() != null && mRow.getCurrentVideo() != null) {
                    mRow.getListener().onOptionsClicked(mRow.getCurrentVideo());
                }
            });

            mNextBtn.setOnClickListener(v -> advanceSlide(1, true));
        }

        private void applyThemeChrome() {
            Context ctx = mCard.getContext();
            int surfaceContainer = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainer, R.color.m3_grey_surface_container);
            int outlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
            int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
            int onSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
            float heroRadius = ctx.getResources().getDimension(R.dimen.m3_hero_corner_radius);
            int hairlinePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

            mCard.setBackground(ViewUtil.createRoundedDrawable(surfaceContainer, outlineVariant, hairlinePx, heroRadius));
            ViewUtil.applyRoundedOutline(mCard, heroRadius);

            GradientDrawable horizScrim = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{
                            surfaceContainer,
                            surfaceContainer,
                            ColorUtils.setAlphaComponent(surfaceContainer, 215),
                            ColorUtils.setAlphaComponent(surfaceContainer, 85),
                            Color.TRANSPARENT
                    });
            mScrimHorizontal.setBackground(horizScrim);

            GradientDrawable vertScrim = new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[]{
                            ColorUtils.setAlphaComponent(surfaceContainer, 230),
                            ColorUtils.setAlphaComponent(surfaceContainer, 70),
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
            renderCurrentSlide(false);
            scheduleAutoRotate();
        }

        void unbind() {
            stopAutoRotate();
            Context ctx = mCard.getContext().getApplicationContext();
            Glide.with(ctx).clear(mArtwork);
            mRow = null;
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

            // Artwork loading
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
        }

        private void renderDots(int count, int activeIndex) {
            mDotsContainer.removeAllViews();
            if (count <= 1) {
                return;
            }
            Context ctx = mDotsContainer.getContext();
            float density = ctx.getResources().getDisplayMetrics().density;
            int activeColor = ViewUtil.getThemeColor(ctx, R.attr.m3Primary, R.color.m3_grey_primary);
            int inactiveColor = ColorUtils.setAlphaComponent(
                    ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant), 110);
            float pillRadius = ctx.getResources().getDimension(R.dimen.m3_pill_corner_radius);

            for (int i = 0; i < count; i++) {
                final int idx = i;
                boolean active = i == activeIndex;
                View dot = new View(ctx);
                int w = Math.round((active ? 22 : 7) * density);
                int h = Math.round(7 * density);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
                if (i > 0) {
                    lp.setMarginStart(Math.round(6 * density));
                }
                dot.setBackground(ViewUtil.createRoundedDrawable(active ? activeColor : inactiveColor, 0, 0, pillRadius));
                dot.setOnClickListener(v -> {
                    if (mRow != null) {
                        mRow.setSelectedIndex(idx);
                        renderCurrentSlide(true);
                        if (mRow.getListener() != null && mRow.getCurrentVideo() != null) {
                            mRow.getListener().onVideoHighlighted(mRow.getCurrentVideo());
                        }
                    }
                });
                mDotsContainer.addView(dot, lp);
            }
        }

        private void scheduleAutoRotate() {
            stopAutoRotate();
            if (mRow != null && mRow.getVideos().size() > 1 && !mAnyButtonFocused) {
                mHandler.postDelayed(mAutoRotateTask, AUTO_ROTATE_INTERVAL_MS);
            }
        }

        private void stopAutoRotate() {
            mHandler.removeCallbacks(mAutoRotateTask);
        }
    }
}
