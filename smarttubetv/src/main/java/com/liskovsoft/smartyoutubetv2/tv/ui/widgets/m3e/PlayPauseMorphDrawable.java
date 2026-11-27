package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.motion.MotionSpec;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;

/**
 * Material 3 Expressive animated Play/Pause morphing drawable.
 * Smoothly morphs and rotates 90 degrees between Play (0.0) and Pause (1.0)
 * using the standard motion tier and easing, while respecting system reduced-motion settings.
 */
public class PlayPauseMorphDrawable extends Drawable {
    private static final float VIRTUAL_SIZE = 24f;
    private static final float HALF_VIRTUAL_SIZE = 12f;

    private final Context mContext;
    private final int mIntrinsicSizePx;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mFirstPath = new Path();
    private final Path mSecondPath = new Path();

    private boolean mInitialized;
    private boolean mShowPause;
    private float mProgress;
    private ValueAnimator mAnimator;

    private int mAlpha = 255;
    private ColorFilter mColorFilter;
    private ColorStateList mTintList;
    private PorterDuff.Mode mTintMode = PorterDuff.Mode.SRC_IN;
    private PorterDuffColorFilter mTintFilter;

    public PlayPauseMorphDrawable(Context context) {
        mContext = context.getApplicationContext();
        float density = context.getResources().getDisplayMetrics().density;
        mIntrinsicSizePx = Math.round(VIRTUAL_SIZE * density);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
    }

    /**
     * Transitions the icon toward Pause ({@code showPause = true}) or Play ({@code showPause = false}).
     */
    public void setShowPause(boolean showPause, boolean animate) {
        if (mInitialized && mShowPause == showPause) {
            return;
        }
        mInitialized = true;
        mShowPause = showPause;
        float target = showPause ? 1f : 0f;

        cancelAnimation();

        if (!animate || !M3Motion.areAnimationsEnabled(mContext)) {
            mProgress = target;
            invalidateSelf();
            return;
        }

        long duration = M3Motion.scaledDuration(M3Motion.DURATION_STANDARD_MS, target - mProgress);
        mAnimator = ValueAnimator.ofFloat(mProgress, target);
        mAnimator.setDuration(duration);
        mAnimator.setInterpolator(M3Motion.standard());
        mAnimator.addUpdateListener(anim -> {
            mProgress = (float) anim.getAnimatedValue();
            invalidateSelf();
        });
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimator == animation) {
                    mAnimator = null;
                    mProgress = target;
                    invalidateSelf();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (mAnimator == animation) {
                    mAnimator = null;
                }
            }
        });
        mAnimator.start();
    }

    public void cancelAnimation() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty() || mAlpha == 0) {
            return;
        }

        float scale = Math.min(bounds.width(), bounds.height()) / VIRTUAL_SIZE;
        float morphT = Math.max(0f, Math.min(1f, mProgress));
        float pulseScale = 1f - MotionSpec.ICON_PULSE_DIP * (float) Math.sin(Math.PI * morphT);

        mPaint.setAlpha(mAlpha);
        mPaint.setColorFilter(mColorFilter != null ? mColorFilter : mTintFilter);

        canvas.save();
        canvas.translate(bounds.exactCenterX(), bounds.exactCenterY());
        canvas.scale(scale * pulseScale, scale * pulseScale);
        canvas.rotate(90f * mProgress);
        canvas.translate(-HALF_VIRTUAL_SIZE, -HALF_VIRTUAL_SIZE);

        if (morphT <= 0.001f) {
            mFirstPath.rewind();
            mFirstPath.moveTo(7f, 5f);
            mFirstPath.lineTo(19f, 12f);
            mFirstPath.lineTo(7f, 19f);
            mFirstPath.close();
            canvas.drawPath(mFirstPath, mPaint);
        } else if (morphT >= 0.999f) {
            mFirstPath.rewind();
            mFirstPath.addRect(5f, 6f, 19f, 10f, Path.Direction.CW);
            mSecondPath.rewind();
            mSecondPath.addRect(5f, 14f, 19f, 18f, Path.Direction.CW);
            canvas.drawPath(mFirstPath, mPaint);
            canvas.drawPath(mSecondPath, mPaint);
        } else {
            float leftX = lerp(7f, 5f, morphT);

            // Top half of Play triangle -> right Pause bar (after +90deg rotation)
            mFirstPath.rewind();
            mFirstPath.moveTo(leftX, lerp(5f, 6f, morphT));
            mFirstPath.lineTo(19f, lerp(12f, 6f, morphT));
            mFirstPath.lineTo(19f, lerp(12f, 10f, morphT));
            mFirstPath.lineTo(leftX, lerp(12f, 10f, morphT));
            mFirstPath.close();
            canvas.drawPath(mFirstPath, mPaint);

            // Bottom half of Play triangle -> left Pause bar (after +90deg rotation)
            mSecondPath.rewind();
            mSecondPath.moveTo(leftX, lerp(12f, 14f, morphT));
            mSecondPath.lineTo(19f, lerp(12f, 14f, morphT));
            mSecondPath.lineTo(19f, lerp(12f, 18f, morphT));
            mSecondPath.lineTo(leftX, lerp(19f, 18f, morphT));
            mSecondPath.close();
            canvas.drawPath(mSecondPath, mPaint);
        }

        canvas.restore();
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicSizePx;
    }

    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicSizePx;
    }

    @Override
    public void setAlpha(int alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mColorFilter = colorFilter;
        invalidateSelf();
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        mTintList = tint;
        updateTintFilter();
        invalidateSelf();
    }

    @Override
    public void setTintMode(@Nullable PorterDuff.Mode tintMode) {
        mTintMode = tintMode != null ? tintMode : PorterDuff.Mode.SRC_IN;
        updateTintFilter();
        invalidateSelf();
    }

    @Override
    public boolean isStateful() {
        return mTintList != null && mTintList.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        return updateTintFilter();
    }

    private boolean updateTintFilter() {
        if (mTintList == null) {
            boolean changed = mTintFilter != null;
            mTintFilter = null;
            return changed;
        }
        int color = mTintList.getColorForState(getState(), mTintList.getDefaultColor());
        mTintFilter = new PorterDuffColorFilter(color, mTintMode);
        return true;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
