package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.leanback.widget.RowPresenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.ui.PlayerView;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the immersive Netflix-style autoplaying M3 Expressive Hero Spotlight Stage
 * at the top of the Home feed with muted TextureView preview, thumbnail-derived banner tone,
 * eased horizontal dissolve, card-consistent focus highlight, and click-to-resume handoff.
 */
public class HeroSpotlightPresenter extends RowPresenter {
    // Centralized tone extraction, surface blend & scrim curve tuning parameters
    private static final float TONE_EDGE_STRIP_FRACTION = 0.22f;
    private static final float TONE_MIN_SATURATION = 0.06f;
    private static final float TONE_MAX_SATURATION = 0.34f;
    private static final float TONE_MIN_LIGHTNESS = 0.08f;
    private static final float TONE_MAX_LIGHTNESS = 0.18f;
    private static final float TONE_SATURATION_SCALE = 0.48f;
    private static final float TONE_LIGHTNESS_SCALE = 0.34f;
    private static final float TONE_SURFACE_BLEND_RATIO = 0.65f; // 65% blend toward theme surface (mScreenBg)

    private static final float[] SCRIM_STOP_OFFSETS = {0f, 0.20f, 0.46f, 0.74f, 1.0f};
    private static final int[] SCRIM_STOP_ALPHAS = {255, 224, 148, 52, 0};
    private static final int TOP_SCRIM_MAX_ALPHA = 112;
    private static final int BOTTOM_SCRIM_END_ALPHA = 216;
    private static final int BOTTOM_SCRIM_MID_ALPHA = 104;

    private static final float MIN_CARD_HEIGHT_DP = 160f;

    private WeakReference<HeroViewHolder> mActiveHolder = new WeakReference<>(null);
    private boolean mIsHostResumed = true;
    private boolean mIsRecyclerDragging = false;
    private boolean mShouldRestoreFocus = false;

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
            if (mShouldRestoreFocus) {
                mShouldRestoreFocus = false;
                vh.restoreFocusIfNeeded();
            }
        }
    }

    public void onHostPause() {
        mIsHostResumed = false;
        HeroViewHolder vh = mActiveHolder.get();
        if (vh != null) {
            mShouldRestoreFocus = vh.hasHeroFocus();
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

    public void onVideosUpdated() {
        HeroViewHolder vh = mActiveHolder.get();
        if (vh != null) {
            vh.refreshPlaylist();
        }
    }

    /**
     * Multi-stop cubic-eased horizontal scrim dissolving the media stage's start edge
     * into the derived banner tone without a linear 2-stop band or hard vertical edge.
     */
    private static final class EasedHorizontalScrimDrawable extends Drawable {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int mColor = Color.TRANSPARENT;
        private float mMediaStartFraction = 0.35f;
        private boolean mIsRtl = false;
        private int mLastWidth = -1;
        private int mLastColor = Color.TRANSPARENT;
        private float mLastStartFraction = -1f;
        private boolean mLastRtl = false;

        void setScrimParams(int color, float mediaStartFraction, boolean isRtl) {
            float clamped = Math.max(0f, Math.min(0.75f, mediaStartFraction));
            if (mColor == color && Math.abs(mMediaStartFraction - clamped) < 0.005f && mIsRtl == isRtl) {
                return;
            }
            mColor = color;
            mMediaStartFraction = clamped;
            mIsRtl = isRtl;
            invalidateSelf();
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            mLastWidth = -1;
        }

        private void ensureShader(int width) {
            if (width <= 0) {
                return;
            }
            if (width == mLastWidth
                    && mColor == mLastColor
                    && Math.abs(mMediaStartFraction - mLastStartFraction) < 0.005f
                    && mIsRtl == mLastRtl) {
                return;
            }
            mLastWidth = width;
            mLastColor = mColor;
            mLastStartFraction = mMediaStartFraction;
            mLastRtl = mIsRtl;

            float fadeStart = mMediaStartFraction;
            float fadeSpan = mMediaStartFraction > 0.05f
                    ? Math.min(0.46f, Math.max(0.28f, 1f - fadeStart - 0.06f))
                    : 0.65f;

            int[] colors = new int[SCRIM_STOP_OFFSETS.length];
            float[] positions = new float[SCRIM_STOP_OFFSETS.length];
            for (int i = 0; i < SCRIM_STOP_OFFSETS.length; i++) {
                colors[i] = ColorUtils.setAlphaComponent(mColor, SCRIM_STOP_ALPHAS[i]);
                positions[i] = Math.min(1f, fadeStart + fadeSpan * SCRIM_STOP_OFFSETS[i]);
            }

            float x0 = mIsRtl ? width : 0f;
            float x1 = mIsRtl ? 0f : width;
            mPaint.setShader(new LinearGradient(x0, 0f, x1, 0f, colors, positions, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) {
                return;
            }
            ensureShader(bounds.width());
            canvas.drawRect(bounds, mPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static final class HeroViewHolder extends RowPresenter.ViewHolder
            implements HeroPreviewController.Listener {
        private final FrameLayout mWrapper;
        private final View mRowAlignmentAnchor;
        private final FrameLayout mCard;
        private final FrameLayout mMediaContainer;
        private final ImageView mArtwork;
        private final PlayerView mPlayerView;
        private final View mScrimHorizontal;
        private final View mScrimTop;
        private final View mScrimVertical;
        private final LinearLayout mContentColumn;
        private final TextView mTitle;
        private final TextView mChannel;
        private final TextView mStats;
        private final LinearLayout mDotsContainer;

        private final HeroPreviewController mPreviewController;
        private final EasedHorizontalScrimDrawable mHorizontalScrimDrawable = new EasedHorizontalScrimDrawable();
        private final LruCache<String, Integer> mToneCache = new LruCache<>(16);
        private final int[] mTempLoc = new int[2];

        private HeroSpotlightRow mRow;
        private View mActiveDotProgressFill;
        private int mActiveDotWidthPx;
        private int mLastCardWidth = -1;
        private int mLastCardHeight = -1;
        private int mPendingMediaWidth = -1;
        private float mMediaStartFraction = 0.35f;

        private boolean mIsTouchInteracting;
        private boolean mIsRecyclerDragging;
        private boolean mScrollListenerAttached;
        private boolean mSwipedHorizontal;
        private boolean mGeometryUpdatePosted;
        private boolean mMediaWidthUpdatePosted;
        private float mTouchDownX;
        private float mTouchDownY;

        private int mScreenBg;
        private int mFallbackTone;
        private int mOutlineColor;
        private int mHairlineColor;
        private float mCornerRadius;
        private int mFocusStrokePx;
        private int mHairlineStrokePx;
        private float mFocusedScale = 1f;

        private int mBrowsePaddingStartPx;
        private int mBrowsePaddingTopPx;
        private int mBrowseRowsMarginTopPx;
        private int mBaselineCardHeightPx;
        private int mHorizontalInsetPx;

        private int mCurrentTone;
        private int mTargetTone;
        private ValueAnimator mToneAnimator;
        private String mActiveImageKey;

        private WeakReference<View> mObservedTitleBar = new WeakReference<>(null);

        private final ViewTreeObserver.OnScrollChangedListener mScrollChangedListener =
                this::updateViewportVisibility;

        private final View.OnLayoutChangeListener mGeometryLayoutListener =
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    if ((right - left) != (oldRight - oldLeft)
                            || (bottom - top) != (oldBottom - oldTop)
                            || top != oldTop) {
                        scheduleGeometryUpdate();
                    }
                };

        private final Runnable mGeometryUpdateRunnable = () -> {
            mGeometryUpdatePosted = false;
            updateDynamicHeroGeometry();
        };

        private final Runnable mMediaWidthRunnable = this::applyPendingMediaWidth;

        private void applyPendingMediaWidth() {
            mMediaWidthUpdatePosted = false;
            if (!ViewCompat.isAttachedToWindow(mMediaContainer) || mPendingMediaWidth <= 0) {
                return;
            }
            ViewGroup.LayoutParams mediaLp = mMediaContainer.getLayoutParams();
            if (mediaLp != null && mediaLp.width != mPendingMediaWidth) {
                mediaLp.width = mPendingMediaWidth;
                mMediaContainer.setLayoutParams(mediaLp);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        HeroViewHolder(View itemView) {
            super(itemView);
            mWrapper = (FrameLayout) itemView;
            mRowAlignmentAnchor = itemView.findViewById(R.id.row_content);
            mCard = itemView.findViewById(R.id.hero_spotlight_card);
            mMediaContainer = itemView.findViewById(R.id.hero_media_container);
            mArtwork = itemView.findViewById(R.id.hero_artwork);
            mPlayerView = itemView.findViewById(R.id.hero_player_view);
            mScrimHorizontal = itemView.findViewById(R.id.hero_scrim_horizontal);
            mScrimTop = itemView.findViewById(R.id.hero_scrim_top);
            mScrimVertical = itemView.findViewById(R.id.hero_scrim_vertical);
            mContentColumn = itemView.findViewById(R.id.hero_content_column);
            mTitle = itemView.findViewById(R.id.hero_title);
            mChannel = itemView.findViewById(R.id.hero_channel);
            mStats = itemView.findViewById(R.id.hero_stats);
            mDotsContainer = itemView.findViewById(R.id.hero_dots_container);

            mMediaContainer.setClipChildren(true);
            mMediaContainer.setClipToPadding(true);

            mPreviewController = new HeroPreviewController(itemView.getContext(), mPlayerView, this);
            mScrimHorizontal.setBackground(mHorizontalScrimDrawable);

            applyThemeTokens();
            mTargetTone = mFallbackTone;
            applyToneImmediate(mFallbackTone);
            applyCardFocusHighlight(false);

            // Single focusable/clickable card matching VideoCardPresenter & FocusHighlightHelper
            mCard.setOnFocusChangeListener((v, hasFocus) -> {
                v.setSelected(hasFocus);
                applyCardFocusHighlight(hasFocus);
                if (hasFocus && mRow != null && mRow.getListener() != null && mRow.getCurrentVideo() != null) {
                    mRow.getListener().onVideoHighlighted(mRow.getCurrentVideo());
                }
            });

            mCard.setOnClickListener(v -> triggerWatch());

            // D-pad Left/Right manual item cycling while preserving Left -> Sidebar at index 0
            mCard.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN || mRow == null || mRow.getVideos().size() <= 1) {
                    return false;
                }
                boolean isRtl = ViewCompat.getLayoutDirection(mCard) == ViewCompat.LAYOUT_DIRECTION_RTL;
                boolean isTowardEnd = (!isRtl && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
                        || (isRtl && keyCode == KeyEvent.KEYCODE_DPAD_LEFT);
                boolean isTowardStart = (!isRtl && keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
                        || (isRtl && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT);

                if (isTowardEnd) {
                    advanceSlide(1, true);
                    return true;
                }
                if (isTowardStart && mRow.getSelectedIndex() > 0) {
                    advanceSlide(-1, true);
                    return true;
                }
                return false;
            });

            final int touchSlop = ViewConfiguration.get(itemView.getContext()).getScaledTouchSlop();
            final float density = itemView.getResources().getDisplayMetrics().density;
            final float swipeThresholdPx = Math.max(touchSlop * 2f, 40f * density);

            mCard.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchDownX = event.getX();
                        mTouchDownY = event.getY();
                        mSwipedHorizontal = false;
                        setTouchInteracting(true);
                        break;
                    case MotionEvent.ACTION_MOVE: {
                        float dx = event.getX() - mTouchDownX;
                        float dy = event.getY() - mTouchDownY;
                        if (!mSwipedHorizontal && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                            mSwipedHorizontal = true;
                            if (mCard.getParent() != null) {
                                mCard.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                        }
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        setTouchInteracting(false);
                        float dx = event.getX() - mTouchDownX;
                        if (mSwipedHorizontal) {
                            mSwipedHorizontal = false;
                            if (Math.abs(dx) >= swipeThresholdPx && mRow != null && mRow.getVideos().size() > 1) {
                                boolean isRtl = ViewCompat.getLayoutDirection(mCard) == ViewCompat.LAYOUT_DIRECTION_RTL;
                                int delta = (dx < 0f) ^ isRtl ? 1 : -1;
                                advanceSlide(delta, true);
                            }
                            return true;
                        }
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL:
                        mSwipedHorizontal = false;
                        setTouchInteracting(false);
                        break;
                }
                return false;
            });

            ViewCompat.setAccessibilityDelegate(mCard, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(Button.class.getName());
                }
            });

            mCard.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                int w = right - left;
                int h = bottom - top;
                adaptResponsiveLayout(w, h);
                updateViewportVisibility();
            });

            mWrapper.addOnLayoutChangeListener(mGeometryLayoutListener);

            ViewCompat.setOnApplyWindowInsetsListener(mWrapper, (v, insets) -> {
                scheduleGeometryUpdate();
                return insets;
            });

            mCard.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    attachScrollObserver();
                    scheduleGeometryUpdate();
                    updateViewportVisibility();
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    detachScrollObserver();
                    detachTitleBarObserver();
                    mCard.removeCallbacks(mGeometryUpdateRunnable);
                    mMediaContainer.removeCallbacks(mMediaWidthRunnable);
                    mGeometryUpdatePosted = false;
                    mMediaWidthUpdatePosted = false;
                    M3Motion.cancelFocusAnimations(mCard);
                    mPreviewController.setHeroVisible(false);
                }
            });
        }

        private void applyThemeTokens() {
            Context ctx = mCard.getContext();
            mScreenBg = ViewUtil.getThemeColor(ctx, R.attr.m3Surface, R.color.m3_grey_surface);
            // Fallback tone on extraction failure matches the theme's surface color
            mFallbackTone = mScreenBg;
            mOutlineColor = ViewUtil.getThemeColor(ctx, R.attr.m3Outline, R.color.m3_grey_outline);
            mHairlineColor = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
            int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
            int onSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);

            mCornerRadius = ctx.getResources().getDimension(R.dimen.lb_rounded_rect_corner_radius);
            mFocusStrokePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);
            mHairlineStrokePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

            mBrowsePaddingStartPx = resolveThemeDimensionPixelSize(
                    ctx, R.attr.browsePaddingStart, R.dimen.lb_browse_padding_start);
            mBrowsePaddingTopPx = resolveThemeDimensionPixelSize(
                    ctx, R.attr.browsePaddingTop, R.dimen.lb_browse_padding_top);
            mBrowseRowsMarginTopPx = resolveThemeDimensionPixelSize(
                    ctx, R.attr.browseRowsMarginTop, R.dimen.lb_browse_rows_margin_top);
            mBaselineCardHeightPx = ctx.getResources().getDimensionPixelSize(R.dimen.hero_spotlight_baseline_height);
            mHorizontalInsetPx = mBrowsePaddingStartPx;

            int estimatedCardWidth = Math.max(1,
                    ctx.getResources().getDisplayMetrics().widthPixels - (mHorizontalInsetPx * 2));
            mFocusedScale = computeClampedFocusScale(estimatedCardWidth, mHorizontalInsetPx);

            ViewUtil.applyRoundedOutline(mCard, mCornerRadius);

            mTitle.setTextColor(onSurface);
            mChannel.setTextColor(ColorUtils.setAlphaComponent(onSurface, 232));
            mStats.setTextColor(onSurfaceVariant);
        }

        private static int resolveThemeDimensionPixelSize(
                @NonNull Context ctx, @AttrRes int attrRes, @DimenRes int fallbackRes) {
            TypedValue tv = new TypedValue();
            if (ctx.getTheme().resolveAttribute(attrRes, tv, true)) {
                if (tv.type == TypedValue.TYPE_DIMENSION) {
                    return TypedValue.complexToDimensionPixelSize(tv.data, ctx.getResources().getDisplayMetrics());
                }
                if (tv.resourceId != 0) {
                    return ctx.getResources().getDimensionPixelSize(tv.resourceId);
                }
            }
            return ctx.getResources().getDimensionPixelSize(fallbackRes);
        }

        private float computeClampedFocusScale(int cardWidth, int horizontalInsetPx) {
            if (cardWidth <= 0) {
                return 1f;
            }
            Context ctx = mCard.getContext();
            float density = ctx.getResources().getDisplayMetrics().density;
            float baseZoom = ctx.getResources().getFraction(R.fraction.lb_focus_zoom_factor_small, 1, 1);
            float refCardWidth = ctx.getResources().getDimension(R.dimen.card_width);
            // Match standard video card radial growth per side while preventing edge/sidebar clipping
            float refExpansionPerSide = refCardWidth * Math.max(0f, baseZoom - 1f) * 0.5f;
            float safeMarginPx = mFocusStrokePx + (4f * density);
            float maxAllowedExpansion = Math.max(0f, horizontalInsetPx - safeMarginPx);
            float expansionPerSide = Math.min(refExpansionPerSide, maxAllowedExpansion);
            return Math.min(baseZoom, 1f + (2f * expansionPerSide) / (float) cardWidth);
        }

        private void scheduleGeometryUpdate() {
            if (!ViewCompat.isAttachedToWindow(mCard) || mGeometryUpdatePosted) {
                return;
            }
            mGeometryUpdatePosted = true;
            mCard.post(mGeometryUpdateRunnable);
        }

        private void detachTitleBarObserver() {
            View oldTitle = mObservedTitleBar.get();
            if (oldTitle != null) {
                oldTitle.removeOnLayoutChangeListener(mGeometryLayoutListener);
            }
            mObservedTitleBar.clear();
        }

        private void updateDynamicHeroGeometry() {
            if (!ViewCompat.isAttachedToWindow(mCard) || mWrapper.getWidth() <= 0) {
                return;
            }

            ViewParent gridParent = mWrapper.getParent();
            if (!(gridParent instanceof View) || ((View) gridParent).getHeight() <= 0) {
                return;
            }
            View gridView = (View) gridParent;
            if (Math.abs(gridView.getScaleY() - 1f) >= 0.01f) {
                return;
            }

            WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(mWrapper);
            Insets safeInsets = rootInsets != null
                    ? rootInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout())
                    : Insets.NONE;

            // Keep left and right insets identical and centered in the collapsed-sidebar content area
            int maxSafeHorizontal = mWrapper.getWidth() / 4;
            int horizontalInset = Math.min(
                    maxSafeHorizontal,
                    Math.max(mBrowsePaddingStartPx, Math.max(safeInsets.left, safeInsets.right)));
            if (mHorizontalInsetPx != horizontalInset
                    || mWrapper.getPaddingStart() != horizontalInset
                    || mWrapper.getPaddingEnd() != horizontalInset) {
                mHorizontalInsetPx = horizontalInset;
                mWrapper.setPaddingRelative(
                        horizontalInset,
                        mWrapper.getPaddingTop(),
                        horizontalInset,
                        mWrapper.getPaddingBottom());
                if (mCard.getWidth() > 0) {
                    updateFocusScaleForWidth(mCard.getWidth());
                }
            }

            View browseFrame = findAncestorById(mWrapper, R.id.browse_frame);
            View searchRoot = browseFrame != null ? browseFrame : mWrapper.getRootView();
            View titleBar = searchRoot != null ? searchRoot.findViewById(R.id.browse_title_group) : null;

            if (titleBar != null && mObservedTitleBar.get() != titleBar) {
                detachTitleBarObserver();
                titleBar.addOnLayoutChangeListener(mGeometryLayoutListener);
                mObservedTitleBar = new WeakReference<>(titleBar);
            }

            // Skip measurement while BrowseSupportFragment hides/translates/fades the title bar
            if (titleBar == null
                    || !ViewCompat.isLaidOut(titleBar)
                    || titleBar.getVisibility() != View.VISIBLE
                    || titleBar.getHeight() <= 0
                    || titleBar.getAlpha() < 0.99f
                    || Math.abs(titleBar.getTranslationY()) >= 1f
                    || Math.abs(titleBar.getScaleY() - 1f) >= 0.01f) {
                return;
            }

            int lowestElementBottom = findLowestVisibleElementBottomInWindow(titleBar, mTempLoc);
            if (lowestElementBottom <= 0) {
                titleBar.getLocationInWindow(mTempLoc);
                lowestElementBottom = mTempLoc[1] + titleBar.getPaddingTop();
            }
            int topBarBottomInWindow = Math.max(lowestElementBottom, safeInsets.top + titleBar.getPaddingTop());

            gridView.getLocationInWindow(mTempLoc);
            int gridTopInWindow = mTempLoc[1];

            float density = mCard.getResources().getDisplayMetrics().density;
            int minCardHeightPx = Math.round(MIN_CARD_HEIGHT_DP * density);
            int maxCardHeightPx = Math.min(
                    Math.max(minCardHeightPx, gridView.getHeight() - mBrowsePaddingTopPx),
                    mBaselineCardHeightPx + Math.round(64f * density));

            // Top sits cleanly below top bar's lowest element by ?attr/browsePaddingTop;
            // bottom remains strictly anchored so rows below never move.
            int desiredCardTopInGrid = (topBarBottomInWindow + mBrowsePaddingTopPx) - gridTopInWindow;
            int fixedCardBottomInGrid = mBrowseRowsMarginTopPx + mBaselineCardHeightPx;
            int targetCardHeight = Math.min(
                    maxCardHeightPx,
                    Math.max(minCardHeightPx, fixedCardBottomInGrid - desiredCardTopInGrid));
            int actualCardTopInGrid = fixedCardBottomInGrid - targetCardHeight;

            int newCardTopMargin = actualCardTopInGrid > mBrowseRowsMarginTopPx
                    ? (actualCardTopInGrid - mBrowseRowsMarginTopPx) : 0;
            int newAnchorTopMargin = actualCardTopInGrid < mBrowseRowsMarginTopPx
                    ? (mBrowseRowsMarginTopPx - actualCardTopInGrid) : 0;

            boolean changed = false;
            ViewGroup.LayoutParams rawCardLp = mCard.getLayoutParams();
            if (rawCardLp instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams cardLp = (FrameLayout.LayoutParams) rawCardLp;
                if (Math.abs(cardLp.height - targetCardHeight) > 1
                        || Math.abs(cardLp.topMargin - newCardTopMargin) > 1) {
                    cardLp.height = targetCardHeight;
                    cardLp.topMargin = newCardTopMargin;
                    mCard.setLayoutParams(cardLp);
                    changed = true;
                }
            }
            if (mRowAlignmentAnchor != null) {
                ViewGroup.LayoutParams rawAnchorLp = mRowAlignmentAnchor.getLayoutParams();
                if (rawAnchorLp instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams anchorLp = (FrameLayout.LayoutParams) rawAnchorLp;
                    if (Math.abs(anchorLp.topMargin - newAnchorTopMargin) > 1) {
                        anchorLp.topMargin = newAnchorTopMargin;
                        mRowAlignmentAnchor.setLayoutParams(anchorLp);
                        changed = true;
                    }
                }
            }
            if (changed) {
                mWrapper.requestLayout();
            }
        }

        @Nullable
        private static View findAncestorById(@NonNull View view, int id) {
            ViewParent parent = view.getParent();
            while (parent instanceof View) {
                View parentView = (View) parent;
                if (parentView.getId() == id) {
                    return parentView;
                }
                parent = parentView.getParent();
            }
            return null;
        }

        private static int findLowestVisibleElementBottomInWindow(@NonNull View root, @NonNull int[] loc) {
            if (root.getVisibility() != View.VISIBLE || root.getAlpha() <= 0.05f
                    || root.getWidth() <= 0 || root.getHeight() <= 0) {
                return 0;
            }
            if (root instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) root;
                int maxBottom = 0;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    int childBottom = findLowestVisibleElementBottomInWindow(vg.getChildAt(i), loc);
                    if (childBottom > maxBottom) {
                        maxBottom = childBottom;
                    }
                }
                return maxBottom;
            }
            if (root instanceof ImageView && ((ImageView) root).getDrawable() == null) {
                return 0;
            }
            if (root instanceof TextView && TextUtils.isEmpty(((TextView) root).getText())) {
                return 0;
            }
            root.getLocationInWindow(loc);
            return loc[1] + root.getHeight();
        }

        private void updateFocusScaleForWidth(int cardWidth) {
            float updatedScale = computeClampedFocusScale(cardWidth, mHorizontalInsetPx);
            if (Math.abs(updatedScale - mFocusedScale) > 0.002f) {
                mFocusedScale = updatedScale;
                if (mCard.hasFocus() && Math.abs(mCard.getScaleX() - mFocusedScale) > 0.002f) {
                    M3Motion.animateFocusScale(mCard, true, mFocusedScale);
                }
            }
        }

        private void applyCardFocusHighlight(boolean focused) {
            M3Motion.animateFocusScale(mCard, focused, mFocusedScale);
            M3Motion.animateFocusElevation(mCard, focused, M3Motion.ELEVATION_REST_DP, M3Motion.ELEVATION_FOCUSED_DP);
            M3Motion.applyCardFocusTransition(
                    mCard,
                    focused,
                    mCurrentTone,
                    mTargetTone,
                    focused ? mHairlineColor : mOutlineColor,
                    focused ? mOutlineColor : mHairlineColor,
                    focused ? mHairlineStrokePx : mFocusStrokePx,
                    focused ? mFocusStrokePx : mHairlineStrokePx,
                    mCornerRadius,
                    null);
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
            updateFocusScaleForWidth(cardWidth);

            Context ctx = mCard.getContext();
            float density = ctx.getResources().getDisplayMetrics().density;

            // Keep 16:9 content filling its container via center-crop / ZOOM without letterboxing
            int ideal16by9Width = Math.round(cardHeight * (16f / 9f));
            int targetMediaWidth = Math.min(cardWidth, Math.max(ideal16by9Width, Math.round(cardWidth * 0.65f)));
            ViewGroup.LayoutParams mediaLp = mMediaContainer.getLayoutParams();
            if (mediaLp != null && mediaLp.width != targetMediaWidth) {
                mPendingMediaWidth = targetMediaWidth;
                if (!mMediaWidthUpdatePosted) {
                    mMediaWidthUpdatePosted = true;
                    mMediaContainer.post(mMediaWidthRunnable);
                }
            }
            mMediaStartFraction = Math.max(0f, (float) (cardWidth - targetMediaWidth) / (float) cardWidth);

            boolean compact = cardWidth < Math.round(520 * density);
            int padH = Math.round((compact ? 18 : 28) * density);
            mContentColumn.setPaddingRelative(
                    padH, mContentColumn.getPaddingTop(), padH, mContentColumn.getPaddingBottom());

            int maxTextWidth = compact
                    ? Math.max(0, cardWidth - (padH * 2))
                    : Math.min(Math.round(460 * density), Math.round(cardWidth * 0.60f));
            mTitle.setMaxWidth(maxTextWidth);
            mChannel.setMaxWidth(maxTextWidth);
            mStats.setMaxWidth(maxTextWidth);

            boolean isRtl = ViewCompat.getLayoutDirection(mCard) == ViewCompat.LAYOUT_DIRECTION_RTL;
            mHorizontalScrimDrawable.setScrimParams(mCurrentTone, mMediaStartFraction, isRtl);
        }

        private void animateToBannerTone(int targetTone) {
            if (mToneAnimator != null) {
                mToneAnimator.cancel();
                mToneAnimator = null;
            }
            mTargetTone = targetTone;
            if (mCurrentTone == targetTone || !M3Motion.areAnimationsEnabled(mCard.getContext())
                    || !ViewCompat.isAttachedToWindow(mCard)) {
                applyToneImmediate(targetTone);
                return;
            }
            final int startTone = mCurrentTone;
            mToneAnimator = ValueAnimator.ofFloat(0f, 1f);
            mToneAnimator.setDuration(M3Motion.DURATION_SLOW_MS);
            mToneAnimator.setInterpolator(M3Motion.standard());
            mToneAnimator.addUpdateListener(anim -> {
                float t = (float) anim.getAnimatedValue();
                applyToneImmediate(ColorUtils.blendARGB(startTone, targetTone, t));
            });
            mToneAnimator.start();
        }

        private void applyToneImmediate(int tone) {
            mCurrentTone = tone;

            Drawable bg = mCard.getBackground();
            if (bg instanceof GradientDrawable) {
                ((GradientDrawable) bg).setColor(tone);
            } else {
                mCard.setBackground(ViewUtil.createRoundedDrawable(tone, 0, 0, mCornerRadius));
            }

            // Keep PlayerView shutter & media backdrop synced with banner tone for seamless crossfades
            mMediaContainer.setBackgroundColor(tone);
            mPlayerView.setShutterBackgroundColor(tone);

            boolean isRtl = ViewCompat.getLayoutDirection(mCard) == ViewCompat.LAYOUT_DIRECTION_RTL;
            mHorizontalScrimDrawable.setScrimParams(tone, mMediaStartFraction, isRtl);

            GradientDrawable topScrim = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            ColorUtils.setAlphaComponent(tone, TOP_SCRIM_MAX_ALPHA),
                            Color.TRANSPARENT
                    });
            mScrimTop.setBackground(topScrim);

            GradientDrawable vertScrim = new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[]{
                            ColorUtils.setAlphaComponent(tone, BOTTOM_SCRIM_END_ALPHA),
                            ColorUtils.setAlphaComponent(tone, BOTTOM_SCRIM_MID_ALPHA),
                            Color.TRANSPARENT
                    });
            mScrimVertical.setBackground(vertScrim);
        }

        private void cancelToneAnimation() {
            if (mToneAnimator != null) {
                mToneAnimator.cancel();
                mToneAnimator = null;
            }
        }

        void bind(@NonNull HeroSpotlightRow row) {
            mRow = row;
            if (ViewCompat.isAttachedToWindow(mCard)) {
                attachScrollObserver();
            }
            renderCurrentSlide(false);
            updateViewportVisibility();
        }

        void refreshPlaylist() {
            if (mRow == null) {
                return;
            }
            List<Video> videos = mRow.getVideos();
            if (videos.isEmpty()) {
                return;
            }
            renderDots(videos.size(), mRow.getSelectedIndex());
            onPlaybackProgress(mPreviewController.getPlaybackProgressFraction());
            Video nextVideo = videos.size() > 1
                    ? videos.get((mRow.getSelectedIndex() + 1) % videos.size())
                    : null;
            mPreviewController.updateNextVideo(nextVideo);
        }

        void unbind() {
            detachScrollObserver();
            detachTitleBarObserver();
            mCard.removeCallbacks(mGeometryUpdateRunnable);
            mMediaContainer.removeCallbacks(mMediaWidthRunnable);
            mGeometryUpdatePosted = false;
            mMediaWidthUpdatePosted = false;
            cancelToneAnimation();
            M3Motion.cancelFocusAnimations(mCard);
            mPreviewController.release();
            Context ctx = mCard.getContext().getApplicationContext();
            Glide.with(ctx).clear(mArtwork);
            mActiveImageKey = null;
            mRow = null;
        }

        void setHostResumed(boolean resumed) {
            mPreviewController.setResumed(resumed);
        }

        void onHostStop() {
            cancelToneAnimation();
            mPreviewController.release();
        }

        void onHostDestroy() {
            detachScrollObserver();
            detachTitleBarObserver();
            mCard.removeCallbacks(mGeometryUpdateRunnable);
            mMediaContainer.removeCallbacks(mMediaWidthRunnable);
            mGeometryUpdatePosted = false;
            mMediaWidthUpdatePosted = false;
            ViewCompat.setOnApplyWindowInsetsListener(mWrapper, null);
            cancelToneAnimation();
            M3Motion.cancelFocusAnimations(mCard);
            mPreviewController.release();
        }

        boolean hasHeroFocus() {
            return mCard.hasFocus();
        }

        void restoreFocusIfNeeded() {
            if (ViewCompat.isAttachedToWindow(mCard) && mCard.isFocusable() && !mCard.hasFocus()) {
                mCard.requestFocus();
            }
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
            mPreviewController.setHeroVisible(isHeroVisibleInViewport());
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
            boolean isRtl = ViewCompat.getLayoutDirection(mDotsContainer) == ViewCompat.LAYOUT_DIRECTION_RTL;
            float width = mActiveDotProgressFill.getWidth() > 0
                    ? mActiveDotProgressFill.getWidth() : mActiveDotWidthPx;
            mActiveDotProgressFill.setPivotX(isRtl ? width : 0f);
            mActiveDotProgressFill.setScaleX(clamped);
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

            String title = video.getTitleFull() != null ? video.getTitleFull() : video.getTitle();
            String channel = video.getAuthor();
            String stats = extractStatsText(video, channel);

            mTitle.setText(title);
            mChannel.setText(channel);
            mChannel.setVisibility(TextUtils.isEmpty(channel) ? View.GONE : View.VISIBLE);
            mStats.setText(stats);
            mStats.setVisibility(TextUtils.isEmpty(stats) ? View.GONE : View.VISIBLE);

            // Single accessibility label combining title and channel (button role exposed via delegate)
            if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(channel)) {
                mCard.setContentDescription(title + ", " + channel);
            } else {
                mCard.setContentDescription(title);
            }

            if (animate) {
                float slidePx = M3Motion.SLIDE_OFFSET_DP * ctx.getResources().getDisplayMetrics().density;
                M3Motion.animateEnter(mTitle, slidePx, 0f);
                M3Motion.animateEnter(mChannel, slidePx, 0f);
                M3Motion.animateEnter(mStats, slidePx, 0f);
            }

            // Apply cached swatch immediately if available; otherwise load bitmap & sample swatch
            int thumbQuality = MainUIData.instance(ctx).getThumbQuality();
            final String primaryUrl = !TextUtils.isEmpty(video.bgImageUrl)
                    ? video.bgImageUrl
                    : ClickbaitRemover.updateThumbnail(video, thumbQuality);
            final boolean isRtl = ViewCompat.getLayoutDirection(mCard) == ViewCompat.LAYOUT_DIRECTION_RTL;
            final String baseCacheKey = !TextUtils.isEmpty(video.videoId) ? video.videoId : primaryUrl;
            final String cacheKey = !TextUtils.isEmpty(baseCacheKey)
                    ? baseCacheKey + (isRtl ? "_rtl" : "_ltr")
                    : null;
            mActiveImageKey = cacheKey;

            Integer cachedTone = !TextUtils.isEmpty(cacheKey) ? mToneCache.get(cacheKey) : null;
            if (cachedTone != null) {
                animateToBannerTone(cachedTone);
            }

            if (!(ctx instanceof Activity) || (Build.VERSION.SDK_INT < 17 || !((Activity) ctx).isDestroyed())) {
                Glide.with(ctx)
                        .load(primaryUrl)
                        .apply(ViewUtil.glideOptions().centerCrop())
                        .diskCacheStrategy(Build.VERSION.SDK_INT > 21 ? DiskCacheStrategy.AUTOMATIC : DiskCacheStrategy.NONE)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(
                                    @Nullable GlideException e,
                                    Object model,
                                    Target<Drawable> target,
                                    boolean isFirstResource) {
                                if (TextUtils.equals(cacheKey, mActiveImageKey) && cachedTone == null) {
                                    animateToBannerTone(mFallbackTone);
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(
                                    Drawable resource,
                                    Object model,
                                    Target<Drawable> target,
                                    DataSource dataSource,
                                    boolean isFirstResource) {
                                if (!TextUtils.equals(cacheKey, mActiveImageKey)) {
                                    return false;
                                }
                                if (cachedTone == null) {
                                    int derived = (resource instanceof BitmapDrawable)
                                            ? extractMutedBannerTone(((BitmapDrawable) resource).getBitmap(), mScreenBg, mFallbackTone, isRtl)
                                            : mFallbackTone;
                                    if (!TextUtils.isEmpty(cacheKey)) {
                                        mToneCache.put(cacheKey, derived);
                                    }
                                    animateToBannerTone(derived);
                                }
                                return false;
                            }
                        })
                        .transition(ViewUtil.glideCrossFade(ctx, M3Motion.DURATION_SLOW_MS))
                        .error(Glide.with(ctx)
                                .load(video.getCardImageUrl())
                                .apply(ViewUtil.glideOptions().centerCrop())
                                .transition(ViewUtil.glideCrossFade(ctx, M3Motion.DURATION_SLOW_MS))
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

        private static String extractStatsText(@NonNull Video video, @Nullable String channel) {
            CharSequence rawSeq = video.getSecondTitleFull() != null ? video.getSecondTitleFull() : video.getSecondTitle();
            String raw = rawSeq != null ? rawSeq.toString().trim() : "";
            if (raw.isEmpty()) {
                return "";
            }
            if (TextUtils.isEmpty(channel)) {
                return raw;
            }
            String[] parts = raw.split(Video.TERTIARY_TEXT_DELIM);
            List<String> kept = new ArrayList<>(parts.length);
            for (String part : parts) {
                String trimmed = part != null ? part.trim() : "";
                if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase(channel.trim())) {
                    kept.add(trimmed);
                }
            }
            return TextUtils.join("  " + Video.TERTIARY_TEXT_DELIM + "  ", kept);
        }

        private static int extractMutedBannerTone(
                @Nullable Bitmap bitmap, int screenBg, int fallbackTone, boolean sampleEndEdge) {
            if (bitmap == null || bitmap.isRecycled()) {
                return fallbackTone;
            }
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width <= 0 || height <= 0) {
                return fallbackTone;
            }

            // Sample only the downscaled edge strip adjacent to the horizontal fade
            int stripWidth = Math.max(1, Math.round(width * TONE_EDGE_STRIP_FRACTION));
            int startX = sampleEndEdge ? Math.max(0, width - stripWidth) : 0;
            int endX = Math.min(width, startX + stripWidth);

            long mutedSumR = 0;
            long mutedSumG = 0;
            long mutedSumB = 0;
            long mutedWeight = 0;

            long allSumR = 0;
            long allSumG = 0;
            long allSumB = 0;
            long allCount = 0;

            float[] hsl = new float[3];
            int stepX = Math.max(1, stripWidth / 8);
            int stepY = Math.max(1, height / 12);

            for (int y = 0; y < height; y += stepY) {
                for (int x = startX; x < endX; x += stepX) {
                    int px = bitmap.getPixel(x, y);
                    if (Color.alpha(px) < 128) {
                        continue;
                    }
                    int r = Color.red(px);
                    int g = Color.green(px);
                    int b = Color.blue(px);
                    allSumR += r;
                    allSumG += g;
                    allSumB += b;
                    allCount++;

                    ColorUtils.colorToHSL(px, hsl);
                    // Prefer dark/muted edge mid-tones and avoid neon highlights
                    if (hsl[1] >= 0.08f && hsl[2] >= 0.05f && hsl[2] <= 0.72f) {
                        int weight = Math.max(1, Math.round((1f - hsl[2]) * 10f));
                        mutedSumR += (long) r * weight;
                        mutedSumG += (long) g * weight;
                        mutedSumB += (long) b * weight;
                        mutedWeight += weight;
                    }
                }
            }

            int baseColor;
            if (mutedWeight > 0) {
                baseColor = Color.rgb(
                        (int) (mutedSumR / mutedWeight),
                        (int) (mutedSumG / mutedWeight),
                        (int) (mutedSumB / mutedWeight));
            } else if (allCount > 0) {
                baseColor = Color.rgb(
                        (int) (allSumR / allCount),
                        (int) (allSumG / allCount),
                        (int) (allSumB / allCount));
            } else {
                return fallbackTone;
            }

            // Clamp HSL saturation and lightness strictly dark/muted, then blend heavily toward theme surface
            ColorUtils.colorToHSL(baseColor, hsl);
            hsl[1] = Math.min(TONE_MAX_SATURATION, Math.max(TONE_MIN_SATURATION, hsl[1] * TONE_SATURATION_SCALE));
            hsl[2] = Math.min(TONE_MAX_LIGHTNESS, Math.max(TONE_MIN_LIGHTNESS, hsl[2] * TONE_LIGHTNESS_SCALE));
            int mutedTone = ColorUtils.HSLToColor(hsl);
            return ColorUtils.blendARGB(mutedTone, screenBg, TONE_SURFACE_BLEND_RATIO);
        }

        private void renderDots(int count, int activeIndex) {
            mDotsContainer.removeAllViews();
            mActiveDotProgressFill = null;
            mActiveDotWidthPx = 0;
            if (count <= 1) {
                mDotsContainer.setVisibility(View.GONE);
                return;
            }
            mDotsContainer.setVisibility(View.VISIBLE);
            Context ctx = mDotsContainer.getContext();
            float density = ctx.getResources().getDisplayMetrics().density;
            int activeColor = ViewUtil.getThemeColor(ctx, R.attr.m3Primary, R.color.m3_grey_primary);
            int inactiveColor = ColorUtils.setAlphaComponent(
                    ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant), 115);
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
                    boolean isRtl = ViewCompat.getLayoutDirection(mDotsContainer) == ViewCompat.LAYOUT_DIRECTION_RTL;
                    fill.setPivotX(isRtl ? w : 0f);
                    fill.setScaleX(0f);
                    pillTrack.addView(fill, new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                    mActiveDotProgressFill = fill;
                    mActiveDotWidthPx = w;
                    dotView = pillTrack;
                } else {
                    View dot = new View(ctx);
                    dot.setBackground(ViewUtil.createRoundedDrawable(inactiveColor, 0, 0, pillRadius));
                    dotView = dot;
                }

                dotView.setFocusable(false);
                dotView.setContentDescription(ctx.getString(R.string.hero_page_indicator, idx + 1, count));
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
                if (active) {
                    M3Motion.animateFadeIn(dotView, M3Motion.DURATION_FAST_MS);
                }
            }
        }
    }
}
