package com.liskovsoft.smartyoutubetv2.tv.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build.VERSION;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.sharedutils.motion.MotionSpec;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Android View animation helpers backed by {@link MotionSpec} (the single source of truth).
 * All animations are non-overshooting, interruptible, reversible, hardware-accelerated
 * (alpha, translation, scale, rotation), and honor system reduced-motion settings.
 */
public final class M3Motion {
    // Re-exported tokens from MotionSpec
    public static final long DURATION_FAST_MS = MotionSpec.DURATION_FAST_MS;
    public static final long DURATION_STANDARD_MS = MotionSpec.DURATION_STANDARD_MS;
    public static final long DURATION_SLOW_MS = MotionSpec.DURATION_SLOW_MS;

    public static final long STAGGER_STEP_MS = MotionSpec.STAGGER_STEP_MS;
    public static final int MAX_STAGGER_ITEMS = MotionSpec.MAX_STAGGER_ITEMS;
    public static final long LOADING_SHOW_DELAY_MS = MotionSpec.LOADING_SHOW_DELAY_MS;
    public static final long HIDE_DEBOUNCE_MS = MotionSpec.HIDE_DEBOUNCE_MS;

    public static final float SLIDE_OFFSET_DP = MotionSpec.SLIDE_OFFSET_DP;
    public static final float SLIDE_OFFSET_COMPACT_DP = MotionSpec.SLIDE_OFFSET_COMPACT_DP;
    public static final float FOCUS_SCALE_SUBTLE = MotionSpec.FOCUS_SCALE_SUBTLE;
    public static final float FOCUS_SCALE_DEFAULT = MotionSpec.FOCUS_SCALE_DEFAULT;
    public static final float FOCUS_SCALE_CARD = MotionSpec.FOCUS_SCALE_CARD;
    public static final float FOCUS_SCALE_ICON = MotionSpec.FOCUS_SCALE_ICON;
    public static final float FOCUS_SCALE_HERO_BUTTON = MotionSpec.FOCUS_SCALE_HERO_BUTTON;
    public static final float PRESS_SCALE = MotionSpec.PRESS_SCALE;
    public static final float DIALOG_ENTER_SCALE = MotionSpec.DIALOG_ENTER_SCALE;
    public static final float ELEVATION_REST_DP = MotionSpec.ELEVATION_REST_DP;
    public static final float ELEVATION_FOCUSED_DP = MotionSpec.ELEVATION_FOCUSED_DP;

    private M3Motion() {
    }

    public static Interpolator standard() {
        return MotionSpec.standard();
    }

    public static Interpolator emphasizedDecelerate() {
        return MotionSpec.emphasizedDecelerate();
    }

    public static Interpolator accelerate() {
        return MotionSpec.accelerate();
    }

    public static boolean areAnimationsEnabled(@Nullable Context context) {
        return MotionSpec.areAnimationsEnabled(context);
    }

    public static long scaledDuration(long baseDurationMs, float normalizedDelta) {
        return MotionSpec.scaledDuration(baseDurationMs, normalizedDelta);
    }

    /**
     * Animates view focus scale using the fast tier and standard easing (no bounce/overshoot).
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
                .setDuration(DURATION_FAST_MS)
                .setInterpolator(standard())
                .start();
    }

    /**
     * Tactile press feedback that briefly scales toward {@link #PRESS_SCALE} without delaying input.
     */
    public static void animatePressScale(@Nullable View view, boolean pressed, float restingScale) {
        if (view == null) {
            return;
        }
        float targetScale = pressed ? restingScale * PRESS_SCALE : restingScale;
        view.animate().cancel();
        if (!areAnimationsEnabled(view.getContext()) || !ViewCompat.isAttachedToWindow(view)) {
            view.setScaleX(targetScale);
            view.setScaleY(targetScale);
            return;
        }
        view.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(DURATION_FAST_MS)
                .setInterpolator(standard())
                .start();
    }

    /**
     * Attaches non-blocking D-pad center/enter and touch press feedback while respecting existing listeners.
     */
    public static void attachPressFeedback(@Nullable View view, float focusedScale) {
        if (view == null) {
            return;
        }
        view.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            float baseScale = v.hasFocus() ? focusedScale : 1.0f;
            if (action == MotionEvent.ACTION_DOWN) {
                animatePressScale(v, true, baseScale);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                animatePressScale(v, false, baseScale);
            }
            return false;
        });
        final View.OnKeyListener existingKey = null;
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                float baseScale = v.hasFocus() ? focusedScale : 1.0f;
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    animatePressScale(v, true, baseScale);
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    animatePressScale(v, false, baseScale);
                }
            }
            return existingKey != null && existingKey.onKey(v, keyCode, event);
        });
    }

    /**
     * Smooth entrance fade + subtle spatial slide using the standard tier and emphasized-decelerate curve.
     */
    public static void animateEnter(@Nullable View view, float startTranslationX, float startTranslationY) {
        animateEnterInternal(view, startTranslationX, startTranslationY, DURATION_STANDARD_MS, 0L);
    }

    /**
     * Staggered first-appearance animation (fade + small upward slide), capped at {@link #MAX_STAGGER_ITEMS}.
     */
    public static void animateEnterStaggered(
            @Nullable View view,
            int itemIndex,
            float startTranslationX,
            float startTranslationY) {
        if (view == null) {
            return;
        }
        if (itemIndex >= MAX_STAGGER_ITEMS || !areAnimationsEnabled(view.getContext())) {
            view.animate().cancel();
            view.setAlpha(1f);
            view.setTranslationX(0f);
            view.setTranslationY(0f);
            return;
        }
        animateEnterInternal(
                view,
                startTranslationX,
                startTranslationY,
                DURATION_STANDARD_MS,
                MotionSpec.staggerDelayMs(itemIndex));
    }

    /**
     * Animates initially visible RecyclerView children (capped at {@link #MAX_STAGGER_ITEMS})
     * once on first non-empty data appearance. Subsequent scroll or pagination updates skip.
     */
    public static void animateInitialVisibleChildren(@Nullable RecyclerView rv) {
        if (rv == null || Boolean.TRUE.equals(rv.getTag(R.id.m3_initial_animated_tag))) {
            return;
        }
        if (!areAnimationsEnabled(rv.getContext())) {
            rv.setTag(R.id.m3_initial_animated_tag, Boolean.TRUE);
            return;
        }
        rv.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rv.getViewTreeObserver().removeOnPreDrawListener(this);
                if (Boolean.TRUE.equals(rv.getTag(R.id.m3_initial_animated_tag))) {
                    return true;
                }
                int count = rv.getChildCount();
                if (count <= 0) {
                    return true;
                }
                rv.setTag(R.id.m3_initial_animated_tag, Boolean.TRUE);
                if (!areAnimationsEnabled(rv.getContext())) {
                    return true;
                }
                float slidePx = SLIDE_OFFSET_DP * rv.getResources().getDisplayMetrics().density;
                int limit = Math.min(count, MAX_STAGGER_ITEMS);
                for (int i = 0; i < limit; i++) {
                    animateEnterStaggered(rv.getChildAt(i), i, 0f, slidePx);
                }
                return true;
            }
        });
    }

    private static void animateEnterInternal(
            @Nullable View view,
            float startTranslationX,
            float startTranslationY,
            long durationMs,
            long startDelayMs) {
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
                .setStartDelay(startDelayMs)
                .setDuration(durationMs)
                .setInterpolator(emphasizedDecelerate())
                .start();
    }

    /**
     * Smooth exit fade + optional slide using the fast tier and accelerate curve.
     */
    public static void animateExit(
            @Nullable View view,
            float endTranslationX,
            float endTranslationY,
            @Nullable Runnable onEnd) {
        if (view == null) {
            if (onEnd != null) {
                onEnd.run();
            }
            return;
        }
        view.animate().cancel();
        if (!areAnimationsEnabled(view.getContext()) || !ViewCompat.isAttachedToWindow(view)) {
            view.setAlpha(1f);
            view.setTranslationX(0f);
            view.setTranslationY(0f);
            if (onEnd != null) {
                onEnd.run();
            }
            return;
        }
        view.animate()
                .alpha(0f)
                .translationX(endTranslationX)
                .translationY(endTranslationY)
                .setDuration(DURATION_FAST_MS)
                .setInterpolator(accelerate())
                .withEndAction(() -> {
                    view.setAlpha(1f);
                    view.setTranslationX(0f);
                    view.setTranslationY(0f);
                    if (onEnd != null) {
                        onEnd.run();
                    }
                })
                .start();
    }

    /**
     * Subtle fade + scale reveal for icon state changes (e.g. toggles, checkmarks).
     */
    public static void animateIconReveal(@Nullable View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        if (!areAnimationsEnabled(view.getContext()) || !ViewCompat.isAttachedToWindow(view)) {
            view.setAlpha(1f);
            view.setScaleX(1f);
            view.setScaleY(1f);
            return;
        }
        view.setAlpha(0f);
        view.setScaleX(0.84f);
        view.setScaleY(0.84f);
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(DURATION_FAST_MS)
                .setInterpolator(standard())
                .start();
    }

    /**
     * Subtle crossfade/scale refresh when an in-place text value (like a setting pill) changes.
     */
    public static void animateTextSwap(@Nullable TextView textView, @Nullable CharSequence newText) {
        if (textView == null) {
            return;
        }
        if (android.text.TextUtils.equals(textView.getText(), newText)) {
            return;
        }
        textView.setText(newText);
        textView.animate().cancel();
        if (!areAnimationsEnabled(textView.getContext()) || !ViewCompat.isAttachedToWindow(textView)) {
            textView.setAlpha(1f);
            textView.setScaleX(1f);
            textView.setScaleY(1f);
            return;
        }
        textView.setAlpha(0.45f);
        textView.setScaleX(0.94f);
        textView.setScaleY(0.94f);
        textView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(DURATION_FAST_MS)
                .setInterpolator(standard())
                .start();
    }

    /**
     * Smooth image alpha reveal for newly loaded thumbnails.
     */
    public static void animateFadeIn(@Nullable View view, long durationMs) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        if (!areAnimationsEnabled(view.getContext()) || !ViewCompat.isAttachedToWindow(view)) {
            view.setAlpha(1f);
            return;
        }
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(durationMs)
                .setInterpolator(emphasizedDecelerate())
                .start();
    }

    /**
     * Cancels running focus color/scale/elevation/corner animators on recycled or unbound views.
     */
    public static void cancelFocusAnimations(@Nullable View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        cancelTagAnimator(view, R.id.m3_focus_color_animator);
        cancelTagAnimator(view, R.id.m3_elevation_animator);
        cancelTagAnimator(view, R.id.m3_corner_morph_animator);
        view.setTag(R.id.m3_focus_state_tag, null);
    }

    /**
     * Cancels focus animators and resets scale back to resting 1.0x when recycling ViewHolders.
     */
    public static void resetOnUnbind(@Nullable View view) {
        if (view == null) {
            return;
        }
        cancelFocusAnimations(view);
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setAlpha(1.0f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
    }

    private static void cancelTagAnimator(View view, int tagId) {
        Object animObj = view.getTag(tagId);
        if (animObj instanceof ValueAnimator) {
            ((ValueAnimator) animObj).cancel();
            view.setTag(tagId, null);
        }
    }

    /**
     * Builds an 8-element corner radii array for segmented grouped list items
     * (rounded outer ends, tight inner corners).
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
     * Animates corner radius smoothly using the fast tier and standard easing.
     */
    public static void animateCornerMorph(
            @Nullable View view,
            @Nullable GradientDrawable drawable,
            float fromRadiusPx,
            float toRadiusPx) {
        if (view == null || drawable == null) {
            return;
        }
        cancelTagAnimator(view, R.id.m3_corner_morph_animator);
        if (Math.abs(fromRadiusPx - toRadiusPx) < 0.5f
                || !areAnimationsEnabled(view.getContext())
                || !ViewCompat.isAttachedToWindow(view)) {
            drawable.setCornerRadius(Math.max(0f, toRadiusPx));
            return;
        }
        ValueAnimator anim = ValueAnimator.ofFloat(fromRadiusPx, toRadiusPx);
        anim.setDuration(DURATION_FAST_MS);
        anim.setInterpolator(standard());
        anim.addUpdateListener(a -> {
            float r = Math.max(0f, (float) a.getAnimatedValue());
            drawable.setCornerRadius(r);
            if (view instanceof FrameLayout && ((FrameLayout) view).getForeground() instanceof GradientDrawable) {
                ((GradientDrawable) ((FrameLayout) view).getForeground()).setCornerRadius(r);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (view.getTag(R.id.m3_corner_morph_animator) == animation) {
                    view.setTag(R.id.m3_corner_morph_animator, null);
                }
            }
        });
        view.setTag(R.id.m3_corner_morph_animator, anim);
        anim.start();
    }

    /**
     * Animates tactile Z elevation using the fast tier (API 21+, no-op on older platforms).
     */
    public static void animateFocusElevation(@Nullable View view, boolean focused, float baseDp, float focusedDp) {
        if (view == null || VERSION.SDK_INT < 21) {
            return;
        }
        float density = view.getResources().getDisplayMetrics().density;
        float targetPx = (focused ? focusedDp : baseDp) * density;
        cancelTagAnimator(view, R.id.m3_elevation_animator);
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
        elevAnim.setDuration(DURATION_FAST_MS);
        elevAnim.setInterpolator(standard());
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
     * using the fast tier and standard easing. Snaps immediately on initial bind or when detached.
     * Pass {@code cornerRadiusPx < 0} when corner radius is driven separately (e.g. via {@link #animateCornerMorph}).
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
        } else if (cornerRadiusPx >= 0f) {
            Object tagRadii = view.getTag(R.id.m3_segmented_radii_tag);
            if (tagRadii instanceof float[]) {
                cornerRadiiPx = (float[]) tagRadii;
            }
        }
        final float[] resolvedRadii = cornerRadiiPx;

        cancelTagAnimator(view, R.id.m3_focus_color_animator);

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
        animator.setDuration(DURATION_FAST_MS);
        animator.setInterpolator(standard());
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
        applyShapeRadii(drawable, Math.max(0f, cornerRadiusPx), cornerRadiiPx);
        return drawable;
    }

    public interface FocusFractionListener {
        void onFraction(float fraction, int currentFillColor);
    }
}
