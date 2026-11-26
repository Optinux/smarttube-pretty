package com.liskovsoft.smartyoutubetv2.tv.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
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
        view.setTag(R.id.m3_focus_state_tag, null);
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
        if (view == null) {
            return;
        }

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
            applySurfaceStep(view, toFillColor, toStrokeColor, toStrokePx, cornerRadiusPx);
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
            applySurfaceStep(view, fill, strokeColor, strokePx, cornerRadiusPx);
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
            float cornerRadiusPx) {
        GradientDrawable bg = ensureGradientDrawable(view.getBackground(), cornerRadiusPx);
        bg.setColor(fillColor);
        if (view.getBackground() != bg) {
            view.setBackground(bg);
        }

        if (view instanceof FrameLayout) {
            FrameLayout fl = (FrameLayout) view;
            if (strokePx > 0 && Color.alpha(strokeColor) > 0) {
                GradientDrawable fg = ensureGradientDrawable(fl.getForeground(), cornerRadiusPx);
                fg.setColor(Color.TRANSPARENT);
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

    private static GradientDrawable ensureGradientDrawable(@Nullable Drawable existing, float cornerRadiusPx) {
        if (existing instanceof GradientDrawable) {
            return (GradientDrawable) existing;
        }
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        if (cornerRadiusPx > 0f) {
            drawable.setCornerRadius(cornerRadiusPx);
        }
        return drawable;
    }

    public interface FocusFractionListener {
        void onFraction(float fraction, int currentFillColor);
    }
}
