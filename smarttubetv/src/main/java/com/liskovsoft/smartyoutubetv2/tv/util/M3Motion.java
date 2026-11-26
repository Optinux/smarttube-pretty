package com.liskovsoft.smartyoutubetv2.tv.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build.VERSION;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Material 3 Expressive motion physics helpers.
 * Spatial animations (scale, translation, size) use spring-derived curves with subtle overshoot,
 * while Effects animations (color, opacity, stroke) strictly use damping 1.0 (no overshoot).
 */
public final class M3Motion {
    public static final long DURATION_FAST_EFFECTS_MS = 150L;
    public static final long DURATION_FAST_SPATIAL_MS = 180L;
    public static final long DURATION_DEFAULT_EFFECTS_MS = 200L;
    public static final long DURATION_ENTER_MS = 220L;
    public static final long DURATION_EXIT_MS = 150L;

    private static final Interpolator FAST_SPATIAL_INTERPOLATOR = createFastSpatialInterpolator();
    private static final Interpolator DEFAULT_SPATIAL_INTERPOLATOR = createDefaultSpatialInterpolator();
    private static final Interpolator FAST_EFFECTS_INTERPOLATOR = createFastEffectsInterpolator();
    private static final Interpolator DEFAULT_EFFECTS_INTERPOLATOR = createDefaultEffectsInterpolator();
    private static final Interpolator EMPHASIZED_DECELERATE_INTERPOLATOR = createEmphasizedDecelerateInterpolator();
    private static final Interpolator EMPHASIZED_ACCELERATE_INTERPOLATOR = createEmphasizedAccelerateInterpolator();

    private M3Motion() {
    }

    public static Interpolator fastSpatial() {
        return FAST_SPATIAL_INTERPOLATOR;
    }

    public static Interpolator defaultSpatial() {
        return DEFAULT_SPATIAL_INTERPOLATOR;
    }

    public static Interpolator fastEffects() {
        return FAST_EFFECTS_INTERPOLATOR;
    }

    public static Interpolator defaultEffects() {
        return DEFAULT_EFFECTS_INTERPOLATOR;
    }

    public static Interpolator emphasizedDecelerate() {
        return EMPHASIZED_DECELERATE_INTERPOLATOR;
    }

    public static Interpolator emphasizedAccelerate() {
        return EMPHASIZED_ACCELERATE_INTERPOLATOR;
    }

    private static Interpolator createFastSpatialInterpolator() {
        if (VERSION.SDK_INT >= 21) {
            return new PathInterpolator(0.42f, 1.67f, 0.21f, 0.90f);
        }
        return new OvershootInterpolator(0.75f);
    }

    private static Interpolator createDefaultSpatialInterpolator() {
        if (VERSION.SDK_INT >= 21) {
            return new PathInterpolator(0.38f, 1.21f, 0.22f, 1.00f);
        }
        return new DecelerateInterpolator(1.5f);
    }

    private static Interpolator createFastEffectsInterpolator() {
        if (VERSION.SDK_INT >= 21) {
            return new PathInterpolator(0.31f, 0.94f, 0.34f, 1.00f);
        }
        return new DecelerateInterpolator();
    }

    private static Interpolator createDefaultEffectsInterpolator() {
        if (VERSION.SDK_INT >= 21) {
            return new PathInterpolator(0.34f, 0.80f, 0.34f, 1.00f);
        }
        return new DecelerateInterpolator();
    }

    private static Interpolator createEmphasizedDecelerateInterpolator() {
        if (VERSION.SDK_INT >= 21) {
            return new PathInterpolator(0.05f, 0.70f, 0.10f, 1.00f);
        }
        return new DecelerateInterpolator(1.8f);
    }

    private static Interpolator createEmphasizedAccelerateInterpolator() {
        if (VERSION.SDK_INT >= 21) {
            return new PathInterpolator(0.30f, 0.00f, 0.80f, 0.15f);
        }
        return new DecelerateInterpolator();
    }

    public static boolean areAnimationsEnabled(@Nullable Context context) {
        if (VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) {
            return false;
        }
        if (context == null) {
            return true;
        }
        if (VERSION.SDK_INT >= 17) {
            try {
                return Settings.Global.getFloat(
                        context.getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE,
                        1.0f) > 0f;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    /**
     * Animates view scale with M3 Expressive Fast Spatial physics, or snaps immediately if animations are disabled.
     */
    public static void animateFocusScale(@Nullable View view, boolean focused, float focusedScale) {
        if (view == null) {
            return;
        }
        float targetScale = focused ? focusedScale : 1.0f;
        view.animate().cancel();
        if (!areAnimationsEnabled(view.getContext()) || !ViewCompat.isAttachedToWindow(view)) {
            view.setScaleX(targetScale);
            view.setScaleY(targetScale);
            return;
        }
        view.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(DURATION_FAST_SPATIAL_MS)
                .setInterpolator(FAST_SPATIAL_INTERPOLATOR)
                .start();
    }

    /**
     * Short, smooth entrance fade + subtle spatial slide for views entering the screen.
     */
    public static void animateEnter(@Nullable View view, float startTranslationX, float startTranslationY) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        if (!areAnimationsEnabled(view.getContext())) {
            view.setAlpha(1f);
            view.setTranslationX(0f);
            view.setTranslationY(0f);
            return;
        }
        view.setAlpha(0f);
        view.setTranslationX(startTranslationX);
        view.setTranslationY(startTranslationY);
        view.animate()
                .alpha(1f)
                .translationX(0f)
                .translationY(0f)
                .setDuration(DURATION_ENTER_MS)
                .setInterpolator(EMPHASIZED_DECELERATE_INTERPOLATOR)
                .start();
    }

    /**
     * Cancels running focus color/scale animators on recycled or unbound card views.
     */
    public static void cancelFocusAnimations(@Nullable View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        Object animObj = view.getTag(R.id.m3_focus_color_animator);
        if (animObj instanceof ValueAnimator) {
            ((ValueAnimator) animObj).cancel();
            view.setTag(R.id.m3_focus_color_animator, null);
        }
        Object elevAnimObj = view.getTag(R.id.m3_elevation_animator);
        if (elevAnimObj instanceof ValueAnimator) {
            ((ValueAnimator) elevAnimObj).cancel();
            view.setTag(R.id.m3_elevation_animator, null);
        }
        view.setTag(R.id.m3_focus_state_tag, null);
    }

    /**
     * Builds an 8-element corner radii array for M3 Expressive segmented grouped list items
     * (large rounded outer ends, tight inner corners).
     */
    public static float[] buildSegmentedRadii(
            float outerRadiusPx,
            float innerRadiusPx,
            boolean isFirst,
            boolean isLast) {
        float top = isFirst ? outerRadiusPx : innerRadiusPx;
        float bottom = isLast ? outerRadiusPx : innerRadiusPx;
        return new float[]{top, top, top, top, bottom, bottom, bottom, bottom};
    }

    /**
     * Animates corner radius smoothly using M3 Fast Spatial physics for state-driven shape morphing.
     */
    public static void animateCornerMorph(
            @Nullable View view,
            @Nullable GradientDrawable drawable,
            float fromRadiusPx,
            float toRadiusPx) {
        if (view == null || drawable == null) {
            return;
        }
        if (Math.abs(fromRadiusPx - toRadiusPx) < 0.5f
                || !areAnimationsEnabled(view.getContext())
                || !ViewCompat.isAttachedToWindow(view)) {
            drawable.setCornerRadius(toRadiusPx);
            return;
        }
        ValueAnimator anim = ValueAnimator.ofFloat(fromRadiusPx, toRadiusPx);
        anim.setDuration(DURATION_FAST_SPATIAL_MS);
        anim.setInterpolator(FAST_SPATIAL_INTERPOLATOR);
        anim.addUpdateListener(a -> {
            float r = (float) a.getAnimatedValue();
            drawable.setCornerRadius(Math.max(0f, r));
            if (view instanceof FrameLayout && ((FrameLayout) view).getForeground() instanceof GradientDrawable) {
                ((GradientDrawable) ((FrameLayout) view).getForeground()).setCornerRadius(Math.max(0f, r));
            }
        });
        anim.start();
    }

    /**
     * Animates tactile Z elevation using M3 Fast Effects (API 21+, no-op on older platforms).
     */
    public static void animateFocusElevation(@Nullable View view, boolean focused, float baseDp, float focusedDp) {
        if (view == null || VERSION.SDK_INT < 21) {
            return;
        }
        float density = view.getResources().getDisplayMetrics().density;
        float targetPx = (focused ? focusedDp : baseDp) * density;
        Object prevElev = view.getTag(R.id.m3_elevation_animator);
        if (prevElev instanceof ValueAnimator) {
            ((ValueAnimator) prevElev).cancel();
        }
        if (!areAnimationsEnabled(view.getContext()) || !ViewCompat.isAttachedToWindow(view)) {
            view.setElevation(targetPx);
            return;
        }
        float startPx = view.getElevation();
        if (Math.abs(startPx - targetPx) < 0.5f) {
            view.setElevation(targetPx);
            return;
        }
        ValueAnimator elevAnim = ValueAnimator.ofFloat(startPx, targetPx);
        elevAnim.setDuration(DURATION_FAST_EFFECTS_MS);
        elevAnim.setInterpolator(FAST_EFFECTS_INTERPOLATOR);
        elevAnim.addUpdateListener(a -> view.setElevation((float) a.getAnimatedValue()));
        view.setTag(R.id.m3_elevation_animator, elevAnim);
        elevAnim.start();
    }

    /**
     * Applies a safe ImageView tint cleanly across API levels without mutating shared drawable state globally.
     */
    public static void setImageTint(@Nullable ImageView imageView, int color) {
        if (imageView == null) {
            return;
        }
        if (VERSION.SDK_INT >= 21) {
            imageView.setImageTintList(ColorStateList.valueOf(color));
        } else {
            imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * Smoothly animates a card's fill color, focus ring foreground, and arbitrary per-frame callback
     * using M3 Fast Effects (no overshoot). Snaps immediately on initial bind or when detached.
     */
    public static void applyCardFocusTransition(
            @Nullable View view,
            boolean focused,
            int fromFillColor,
            int toFillColor,
            int fromStrokeColor,
            int toStrokeColor,
            int fromStrokePx,
            int toStrokePx,
            float cornerRadiusPx,
            @Nullable FocusFractionListener frameListener) {
        applyCardFocusTransition(
                view, focused, fromFillColor, toFillColor, fromStrokeColor, toStrokeColor,
                fromStrokePx, toStrokePx, cornerRadiusPx, null, frameListener);
    }

    /**
     * Smoothly animates a card or segmented list row supporting optional 8-element per-corner radii.
     */
    public static void applyCardFocusTransition(
            @Nullable View view,
            boolean focused,
            int fromFillColor,
            int toFillColor,
            int fromStrokeColor,
            int toStrokeColor,
            int fromStrokePx,
            int toStrokePx,
            float cornerRadiusPx,
            @Nullable float[] cornerRadiiPx,
            @Nullable FocusFractionListener frameListener) {
        if (view == null) {
            return;
        }

        if (cornerRadiiPx != null) {
            view.setTag(R.id.m3_segmented_radii_tag, cornerRadiiPx);
        } else {
            Object tagRadii = view.getTag(R.id.m3_segmented_radii_tag);
            if (tagRadii instanceof float[]) {
                cornerRadiiPx = (float[]) tagRadii;
            }
        }
        final float[] resolvedRadii = cornerRadiiPx;

        Object prevAnim = view.getTag(R.id.m3_focus_color_animator);
        if (prevAnim instanceof ValueAnimator) {
            ((ValueAnimator) prevAnim).cancel();
            view.setTag(R.id.m3_focus_color_animator, null);
        }

        Boolean prevState = (Boolean) view.getTag(R.id.m3_focus_state_tag);
        view.setTag(R.id.m3_focus_state_tag, focused);

        boolean shouldAnimate = prevState != null
                && prevState != focused
                && ViewCompat.isAttachedToWindow(view)
                && areAnimationsEnabled(view.getContext());

        if (!shouldAnimate) {
            applySurfaceStep(view, toFillColor, toStrokeColor, toStrokePx, cornerRadiusPx, resolvedRadii);
            if (frameListener != null) {
                frameListener.onFraction(1f, toFillColor);
            }
            return;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_FAST_EFFECTS_MS);
        animator.setInterpolator(FAST_EFFECTS_INTERPOLATOR);
        animator.addUpdateListener(anim -> {
            float t = (float) anim.getAnimatedValue();
            int fill = ColorUtils.blendARGB(fromFillColor, toFillColor, t);
            int strokeColor = ColorUtils.blendARGB(fromStrokeColor, toStrokeColor, t);
            int strokePx = Math.round(fromStrokePx + (toStrokePx - fromStrokePx) * t);
            applySurfaceStep(view, fill, strokeColor, strokePx, cornerRadiusPx, resolvedRadii);
            if (frameListener != null) {
                frameListener.onFraction(t, fill);
            }
        });
        view.setTag(R.id.m3_focus_color_animator, animator);
        animator.start();
    }

    private static void applySurfaceStep(
            View view,
            int fillColor,
            int strokeColor,
            int strokePx,
            float cornerRadiusPx,
            @Nullable float[] cornerRadiiPx) {
        GradientDrawable bg = ensureGradientDrawable(view.getBackground(), cornerRadiusPx, cornerRadiiPx);
        bg.setColor(fillColor);
        applyShapeRadii(bg, cornerRadiusPx, cornerRadiiPx);
        if (view.getBackground() != bg) {
            view.setBackground(bg);
        }

        if (view instanceof FrameLayout) {
            FrameLayout fl = (FrameLayout) view;
            if (strokePx > 0 && Color.alpha(strokeColor) > 0) {
                GradientDrawable fg = ensureGradientDrawable(fl.getForeground(), cornerRadiusPx, cornerRadiiPx);
                fg.setColor(Color.TRANSPARENT);
                applyShapeRadii(fg, cornerRadiusPx, cornerRadiiPx);
                fg.setStroke(strokePx, strokeColor);
                if (fl.getForeground() != fg) {
                    fl.setForeground(fg);
                }
            } else if (fl.getForeground() != null) {
                fl.setForeground(null);
            }
        } else if (strokePx > 0 && Color.alpha(strokeColor) > 0) {
            bg.setStroke(strokePx, strokeColor);
        } else {
            bg.setStroke(0, Color.TRANSPARENT);
        }
    }

    private static void applyShapeRadii(
            GradientDrawable drawable,
            float cornerRadiusPx,
            @Nullable float[] cornerRadiiPx) {
        if (cornerRadiiPx != null && cornerRadiiPx.length == 8) {
            drawable.setCornerRadii(cornerRadiiPx);
        } else if (cornerRadiusPx >= 0f) {
            drawable.setCornerRadius(cornerRadiusPx);
        }
    }

    private static GradientDrawable ensureGradientDrawable(
            @Nullable Drawable existing,
            float cornerRadiusPx,
            @Nullable float[] cornerRadiiPx) {
        if (existing instanceof GradientDrawable) {
            return (GradientDrawable) existing;
        }
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        applyShapeRadii(drawable, cornerRadiusPx, cornerRadiiPx);
        return drawable;
    }

    public interface FocusFractionListener {
        void onFraction(float fraction, int currentFillColor);
    }
}
