/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.misc;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.leanback.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Replacement of SeekBar, has two bar heights and two thumb size when focused/not_focused.
 * The widget does not deal with KeyEvent, it's client's responsibility to set a key listener.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class SeekBar extends View {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public abstract static class AccessibilitySeekListener {
        /**
         * Called to perform AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
         */
        public abstract boolean onAccessibilitySeekForward();
        /**
         * Called to perform AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
         */
        public abstract boolean onAccessibilitySeekBackward();
        /**
         * Touch event handling
         */
        public abstract boolean onAccessibilitySeekProgress(int progress);
    }

    private static class SeekBarRectangle {
        public final RectF rect = new RectF();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    private final RectF mProgressRect = new RectF();
    private final RectF mSecondProgressRect = new RectF();
    private final RectF mBackgroundRect = new RectF();
    private final RectF mKnobRect = new RectF();
    private final RectF mHaloRect = new RectF();
    private final RectF mShadowRect = new RectF();
    private final Path mTrackClipPath = new Path();
    private final Paint mSecondProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnobHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnobShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStopIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mProgress;
    private int mSecondProgress;
    private int mMax;
    private int mKnobx;

    private int mActiveRadius;
    private int mBarHeight;
    private int mActiveBarHeight;
    private final float mShadowOffsetPx;
    private final float mShadowRadiusExtraPx;
    private final float mStopIndicatorRadiusPx;
    private final float mHandleWidthPx;
    private final float mHandleHeightPx;
    private final float mHandleGapPx;
    private final int mUnfocusedKnobColor;
    private final int mFocusedKnobColor;
    private int mHaloBaseColor;
    private float mFocusFraction;
    private ValueAnimator mFocusAnimator;
    private final List<SeekBarRectangle> mSeekBarRectangles = new ArrayList<>();
    private List<SeekBarSegment> mSegments;

    private AccessibilitySeekListener mAccessibilitySeekListener;

    public SeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        int trackBase = ViewUtil.getThemeColor(
                context, com.liskovsoft.smartyoutubetv2.tv.R.attr.m3OnSurfaceVariant,
                com.liskovsoft.smartyoutubetv2.tv.R.color.m3_seekbar_track);
        mBackgroundPaint.setColor(Color.argb(
                84, Color.red(trackBase), Color.green(trackBase), Color.blue(trackBase)));
        mSecondProgressPaint.setColor(ViewUtil.getThemeColor(
                context, R.attr.playbackProgressSecondaryColor,
                com.liskovsoft.smartyoutubetv2.tv.R.color.m3_seekbar_buffer));
        mProgressPaint.setColor(ViewUtil.getThemeColor(
                context, R.attr.playbackProgressPrimaryColor,
                com.liskovsoft.smartyoutubetv2.tv.R.color.m3_grey_accent_red));
        mUnfocusedKnobColor = ViewUtil.getThemeColor(
                context, com.liskovsoft.smartyoutubetv2.tv.R.attr.m3OnSurface,
                com.liskovsoft.smartyoutubetv2.tv.R.color.m3_seekbar_knob);
        mFocusedKnobColor = Color.WHITE;
        mKnobPaint.setColor(mUnfocusedKnobColor);
        mHaloBaseColor = ViewUtil.getThemeColor(
                context, com.liskovsoft.smartyoutubetv2.tv.R.attr.m3Primary,
                com.liskovsoft.smartyoutubetv2.tv.R.color.m3_seekbar_knob);
        mKnobHaloPaint.setColor(Color.argb(
                0, Color.red(mHaloBaseColor), Color.green(mHaloBaseColor), Color.blue(mHaloBaseColor)));
        mStopIndicatorPaint.setColor(mProgressPaint.getColor());
        mKnobShadowPaint.setColor(0x4D000000);
        float density = context.getResources().getDisplayMetrics().density;
        mShadowOffsetPx = 0.75f * density;
        mShadowRadiusExtraPx = 1.25f * density;
        mStopIndicatorRadiusPx = 2f * density; // 4dp M3 Expressive end stop indicator
        mHandleWidthPx = 4.5f * density;
        mHandleHeightPx = 22f * density;
        mHandleGapPx = 3.5f * density;
        mBarHeight = context.getResources().getDimensionPixelSize(
                R.dimen.lb_playback_transport_progressbar_bar_height);
        mActiveBarHeight = context.getResources().getDimensionPixelSize(
                R.dimen.lb_playback_transport_progressbar_active_bar_height);
        mActiveRadius = context.getResources().getDimensionPixelSize(
                R.dimen.lb_playback_transport_progressbar_active_radius);
    }

    /**
     * Set radius in pixels for thumb when SeekBar is focused.
     */
    public void setActiveRadius(int radius) {
        mActiveRadius = radius;
        calculate();
    }

    /**
     * Set horizontal bar height in pixels when SeekBar is not focused.
     */
    public void setBarHeight(int barHeight) {
        mBarHeight = barHeight;
        calculate();
    }

    /**
     * Set horizontal bar height in pixels when SeekBar is focused.
     */
    public void setActiveBarHeight(int activeBarHeight) {
        mActiveBarHeight = activeBarHeight;
        calculate();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus,
            int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        animateFocus(gainFocus);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mFocusAnimator != null) {
            mFocusAnimator.cancel();
            mFocusAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    private void animateFocus(boolean gainFocus) {
        if (mFocusAnimator != null) {
            mFocusAnimator.cancel();
            mFocusAnimator = null;
        }
        float target = gainFocus ? 1f : 0f;
        if (!M3Motion.areAnimationsEnabled(getContext()) || !ViewCompat.isAttachedToWindow(this)) {
            mFocusFraction = target;
            calculate();
            return;
        }
        mFocusAnimator = ValueAnimator.ofFloat(mFocusFraction, target);
        mFocusAnimator.setDuration(M3Motion.DURATION_FAST_SPATIAL_MS);
        mFocusAnimator.setInterpolator(M3Motion.fastSpatial());
        mFocusAnimator.addUpdateListener(animation -> {
            mFocusFraction = (float) animation.getAnimatedValue();
            calculate();
        });
        mFocusAnimator.start();
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculate();
        if (mSegments != null) {
            calculateSegments(mSegments);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float clampedFraction = Math.max(0f, Math.min(1f, mFocusFraction));
        final float barHeight = mBarHeight + (mActiveBarHeight - mBarHeight) * clampedFraction;
        final float barRadius = barHeight / 2f;
        final float centerY = getHeight() / 2f;

        // Morph resting dot -> tall M3 Expressive vertical pill handle with track-handle gap
        final float minimalRadius = mBarHeight / 2f + 1f;
        final float restingDiameter = minimalRadius * 2f;
        final float halfW = (restingDiameter + (mHandleWidthPx - restingDiameter) * clampedFraction) / 2f;
        final float halfH = Math.min(
                centerY - mShadowRadiusExtraPx,
                (restingDiameter + (mHandleHeightPx - restingDiameter) * clampedFraction) / 2f);
        final float handleCornerR = Math.min(halfW, halfH);
        final float gapSize = mHandleGapPx * clampedFraction;
        final float gapLeft = mKnobx - halfW - gapSize;
        final float gapRight = mKnobx + halfW + gapSize;

        // Draw track layers, carving a transparent cutout gap around the focused vertical pill handle
        drawTrackSlice(canvas, mBackgroundRect.left, Math.min(mBackgroundRect.right, gapLeft), barRadius);
        if (gapSize > 0.5f && gapRight < mBackgroundRect.right) {
            drawTrackSlice(canvas, Math.max(mBackgroundRect.left, gapRight), mBackgroundRect.right, barRadius);
        }

        // M3 Expressive 4dp end stop indicator when progress has not reached the trailing end
        if (mMax > 0 && mProgress < mMax * 0.985f) {
            float stopX = mBackgroundRect.right - barRadius;
            if (stopX > gapRight + mStopIndicatorRadiusPx) {
                canvas.drawCircle(stopX, centerY, mStopIndicatorRadiusPx, mStopIndicatorPaint);
            }
        }

        mKnobPaint.setColor(androidx.core.graphics.ColorUtils.blendARGB(
                mUnfocusedKnobColor, mFocusedKnobColor, clampedFraction));

        if (clampedFraction > 0.01f) {
            float haloPad = (halfW + 3f * clampedFraction) + mShadowRadiusExtraPx;
            float haloHalfH = Math.min(centerY, halfH + 3f * clampedFraction);
            mHaloRect.set(mKnobx - haloPad, centerY - haloHalfH, mKnobx + haloPad, centerY + haloHalfH);
            int haloAlpha = Math.round(72 * clampedFraction);
            mKnobHaloPaint.setColor(Color.argb(
                    haloAlpha, Color.red(mHaloBaseColor), Color.green(mHaloBaseColor), Color.blue(mHaloBaseColor)));
            canvas.drawRoundRect(mHaloRect, haloPad, haloPad, mKnobHaloPaint);

            mShadowRect.set(
                    mKnobx - halfW - mShadowRadiusExtraPx,
                    centerY - halfH + mShadowOffsetPx - mShadowRadiusExtraPx * 0.4f,
                    mKnobx + halfW + mShadowRadiusExtraPx,
                    centerY + halfH + mShadowOffsetPx + mShadowRadiusExtraPx * 0.6f);
            int shadowAlpha = Math.round(77 * clampedFraction);
            mKnobShadowPaint.setColor(Color.argb(shadowAlpha, 0, 0, 0));
            canvas.drawRoundRect(mShadowRect, handleCornerR + mShadowRadiusExtraPx, handleCornerR + mShadowRadiusExtraPx, mKnobShadowPaint);
        }

        mKnobRect.set(mKnobx - halfW, centerY - halfH, mKnobx + halfW, centerY + halfH);
        canvas.drawRoundRect(mKnobRect, handleCornerR, handleCornerR, mKnobPaint);
    }

    private void drawTrackSlice(Canvas canvas, float sliceLeft, float sliceRight, float barRadius) {
        if (sliceRight <= sliceLeft) {
            return;
        }
        final int outerSave = canvas.save();
        mTrackClipPath.reset();
        mTrackClipPath.addRoundRect(mBackgroundRect, barRadius, barRadius, Path.Direction.CW);
        canvas.clipPath(mTrackClipPath);
        canvas.clipRect(sliceLeft, 0, sliceRight, getHeight());

        canvas.drawRoundRect(mBackgroundRect, barRadius, barRadius, mBackgroundPaint);
        if (mSecondProgressRect.right > mSecondProgressRect.left) {
            canvas.drawRoundRect(mSecondProgressRect, barRadius, barRadius, mSecondProgressPaint);
        }
        if (mProgressRect.right > mProgressRect.left) {
            canvas.drawRoundRect(mProgressRect, barRadius, barRadius, mProgressPaint);
        }
        for (SeekBarRectangle rectangle : mSeekBarRectangles) {
            canvas.drawRect(rectangle.rect, rectangle.paint);
        }
        canvas.restoreToCount(outerSave);
    }

    /**
     * Set progress within 0 and {@link #getMax()}
     */
    public void setProgress(int progress) {
        if (progress > mMax) {
            progress = mMax;
        } else if (progress < 0) {
            progress = 0;
        }
        mProgress = progress;
        calculate();
    }

    /**
     * Set secondary progress within 0 and {@link #getMax()}
     */
    public void setSecondaryProgress(int progress) {
        if (progress > mMax) {
            progress = mMax;
        } else if (progress < 0) {
            progress = 0;
        }
        mSecondProgress = progress;
        calculate();
    }

    /**
     * Get progress within 0 and {@link #getMax()}
     */
    public int getProgress() {
        return mProgress;
    }

    /**
     * Get secondary progress within 0 and {@link #getMax()}
     */
    public int getSecondProgress() {
        return mSecondProgress;
    }

    /**
     * Get max value.
     */
    public int getMax() {
        return mMax;
    }

    /**
     * Set max value.
     */
    public void setMax(int max) {
        this.mMax = max;
        calculate();
    }

    /**
     * Set color for progress.
     */
    public void setProgressColor(int color) {
        mProgressPaint.setColor(color);
        mStopIndicatorPaint.setColor(color);
        mHaloBaseColor = color;
    }

    /**
     * Set color for second progress which is usually for buffering indication.
     */
    public void setSecondaryProgressColor(int color) {
        mSecondProgressPaint.setColor(color);
    }

    /**
     * Set color for second progress which is usually for buffering indication.
     */
    public int getSecondaryProgressColor() {
        return mSecondProgressPaint.getColor();
    }

    private void calculate() {
        final float clampedFraction = Math.max(0f, Math.min(1f, mFocusFraction));
        final float barHeight = mBarHeight + (mActiveBarHeight - mBarHeight) * clampedFraction;

        final int width = getWidth();
        final int height = getHeight();
        final float verticalPadding = (height - barHeight) / 2f;

        mBackgroundRect.set(mBarHeight / 2f, verticalPadding,
                width - mBarHeight / 2f, height - verticalPadding);

        final float radius = (mBarHeight / 2f) + (mActiveRadius - mBarHeight / 2f) * clampedFraction;
        final float progressWidth = Math.max(0f, width - radius * 2f);
        final float progressPixels = mMax > 0 ? mProgress / (float) mMax * progressWidth : 0f;
        mKnobx = Math.round(radius + progressPixels);
        mProgressRect.set(mBarHeight / 2f, verticalPadding,
                mProgress > 0 ? Math.max(mBarHeight / 2f, mKnobx) : mBarHeight / 2f,
                height - verticalPadding);

        final float secondProgressPixels = mMax > 0 ? mSecondProgress / (float) mMax * progressWidth : 0f;
        final float secondRight = mSecondProgress > 0
                ? Math.min(width - mBarHeight / 2f, radius + secondProgressPixels)
                : mProgressRect.right;
        mSecondProgressRect.set(mProgressRect.right, verticalPadding,
                Math.max(mProgressRect.right, secondRight), height - verticalPadding);

        for (SeekBarRectangle seekBarRectangle : mSeekBarRectangles) {
            seekBarRectangle.rect.set(seekBarRectangle.rect.left, verticalPadding, seekBarRectangle.rect.right, height - verticalPadding);
        }

        invalidate();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return SeekBar.class.getName();
    }

    public void setAccessibilitySeekListener(AccessibilitySeekListener listener) {
        mAccessibilitySeekListener = listener;
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (mAccessibilitySeekListener != null) {
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                    return mAccessibilitySeekListener.onAccessibilitySeekForward();
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
                    return mAccessibilitySeekListener.onAccessibilitySeekBackward();
            }
        }
        return super.performAccessibilityAction(action, arguments);
    }

    public void setSegments(List<SeekBarSegment> segments) {
        mSegments = segments;
        calculateSegments(segments);
    }

    private void calculateSegments(List<SeekBarSegment> segments) {
        mSeekBarRectangles.clear();
        if (segments == null || getWidth() <= 0) {
            invalidate();
            return;
        }

        final float clampedFraction = Math.max(0f, Math.min(1f, mFocusFraction));
        final float barHeight = mBarHeight + (mActiveBarHeight - mBarHeight) * clampedFraction;

        final int width = getWidth();
        final int height = getHeight();
        final float verticalPadding = (height - barHeight) / 2f;

        //final int radius = isFocused() ? mActiveRadius : mBarHeight / 2;
        //final int progressWidth = width - radius * 2;

        for (SeekBarSegment segment : segments) {
            if (segment.endProgress < 0 || segment.startProgress < 0 || segment.startProgress >= 1) {
                continue;
            }

            if (segment.endProgress > 1) {
                segment.endProgress = 1;
            }

            float rightPixels = segment.endProgress * width;
            float leftPixels = segment.startProgress * width;

            // Bookmark segment (1px width) fix
            float bookmarkWidth = mBarHeight / 2f;
            if (rightPixels - leftPixels < bookmarkWidth) {
                rightPixels += bookmarkWidth;
            }

            SeekBarRectangle rect = new SeekBarRectangle();
            rect.rect.set(leftPixels, verticalPadding, rightPixels, height - verticalPadding);
            rect.paint.setColor(segment.color);
            mSeekBarRectangles.add(rect);
        }

        invalidate();
    }

    // Touch interceptor

    private int mScaledTouchSlop;
    private int mPaddingLeft;
    private int mPaddingRight;
    private int mTouchProgressOffset;
    private float mTouchDownX;
    private boolean mIsDragging;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = event.getX();
                startDrag(event);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                } else {
                    final float x = event.getX();
                    if (Math.abs(x - mTouchDownX) > mScaledTouchSlop) {
                        startDrag(event);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold should
                    // be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private void startDrag(MotionEvent event) {
        setPressed(true);

        onStartTrackingTouch();
        trackTouchEvent(event);
        attemptClaimDrag();
    }

    private void setHotspot(float x, float y) {
        if (VERSION.SDK_INT < 21) {
            return;
        }

        final Drawable bg = getBackground();
        if (bg != null) {
            bg.setHotspot(x, y);
        }
    }
    
    private void trackTouchEvent(MotionEvent event) {
        final int x = Math.round(event.getX());
        final int y = Math.round(event.getY());
        final int width = getWidth();
        final int availableWidth = width;

        final float scale;
        float progress = 0.0f;
        if (x < mPaddingLeft) {
            scale = 0.0f;
        } else if (x > width - mPaddingRight) {
            scale = 1.0f;
        } else {
            scale = (x - mPaddingLeft) / (float) availableWidth;
            progress = mTouchProgressOffset;
        }

        final int range = getMax() - getMin();
        progress += scale * range + getMin();

        setHotspot(x, y);
        setProgress(Math.round(progress));
        if (mAccessibilitySeekListener != null) {
            mAccessibilitySeekListener.onAccessibilitySeekProgress(mProgress);
        }
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        //if (mParent != null) {
        //    mParent.requestDisallowInterceptTouchEvent(true);
        //}
    }

    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    /**
     * This is called when the user either releases his touch or the touch is
     * canceled.
     */
    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    public synchronized int getMin() {
        return 0;
    }
}
