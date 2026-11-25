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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.leanback.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;
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
    private final Path mTrackClipPath = new Path();
    private final Paint mSecondProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnobHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnobShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mProgress;
    private int mSecondProgress;
    private int mMax;
    private int mKnobx;

    private int mActiveRadius;
    private int mBarHeight;
    private int mActiveBarHeight;
    private final float mShadowOffsetPx;
    private final float mShadowRadiusExtraPx;
    private final List<SeekBarRectangle> mSeekBarRectangles = new ArrayList<>();
    private List<SeekBarSegment> mSegments;

    private AccessibilitySeekListener mAccessibilitySeekListener;

    public SeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        mBackgroundPaint.setColor(ContextCompat.getColor(
                context, com.liskovsoft.smartyoutubetv2.tv.R.color.m3_seekbar_track));
        mSecondProgressPaint.setColor(ViewUtil.getThemeColor(
                context, R.attr.playbackProgressSecondaryColor,
                com.liskovsoft.smartyoutubetv2.tv.R.color.m3_seekbar_buffer));
        mProgressPaint.setColor(ViewUtil.getThemeColor(
                context, R.attr.playbackProgressPrimaryColor,
                com.liskovsoft.smartyoutubetv2.tv.R.color.m3_grey_accent_red));
        mKnobPaint.setColor(ContextCompat.getColor(
                context, com.liskovsoft.smartyoutubetv2.tv.R.color.m3_seekbar_knob));
        int haloBaseColor = ViewUtil.getThemeColor(
                context, com.liskovsoft.smartyoutubetv2.tv.R.attr.m3Primary,
                com.liskovsoft.smartyoutubetv2.tv.R.color.m3_seekbar_knob);
        mKnobHaloPaint.setColor(Color.argb(
                76, Color.red(haloBaseColor), Color.green(haloBaseColor), Color.blue(haloBaseColor)));
        mKnobShadowPaint.setColor(0x4D000000);
        float density = context.getResources().getDisplayMetrics().density;
        mShadowOffsetPx = 0.75f * density;
        mShadowRadiusExtraPx = 1f * density;
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
        calculate();
        //calculateSegments();
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
        final float barRadius = (isFocused() ? mActiveBarHeight : mBarHeight) / 2f;
        canvas.drawRoundRect(mBackgroundRect, barRadius, barRadius, mBackgroundPaint);

        mTrackClipPath.reset();
        mTrackClipPath.addRoundRect(mBackgroundRect, barRadius, barRadius, Path.Direction.CW);
        final int saveCount = canvas.save();
        canvas.clipPath(mTrackClipPath);
        if (mSecondProgressRect.right > mSecondProgressRect.left) {
            canvas.drawRoundRect(mSecondProgressRect, barRadius, barRadius, mSecondProgressPaint);
        }
        if (mProgressRect.right > mProgressRect.left) {
            canvas.drawRoundRect(mProgressRect, barRadius, barRadius, mProgressPaint);
        }
        for (SeekBarRectangle rectangle : mSeekBarRectangles) {
            canvas.drawRect(rectangle.rect, rectangle.paint);
        }
        canvas.restoreToCount(saveCount);

        final float centerY = getHeight() / 2f;
        if (isFocused()) {
            final float haloRadius = Math.min(mActiveRadius * 1.6f, centerY);
            canvas.drawCircle(mKnobx, centerY, haloRadius, mKnobHaloPaint);
            canvas.drawCircle(mKnobx, centerY + mShadowOffsetPx, mActiveRadius + mShadowRadiusExtraPx, mKnobShadowPaint);
            canvas.drawCircle(mKnobx, centerY, mActiveRadius, mKnobPaint);
        } else {
            final float minimalRadius = mBarHeight / 2f + 1f;
            canvas.drawCircle(mKnobx, centerY, minimalRadius, mKnobPaint);
        }
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
        final int barHeight = isFocused() ? mActiveBarHeight : mBarHeight;

        final int width = getWidth();
        final int height = getHeight();
        final int verticalPadding = (height - barHeight) / 2;

        mBackgroundRect.set(mBarHeight / 2f, verticalPadding,
                width - mBarHeight / 2f, height - verticalPadding);

        final int radius = isFocused() ? mActiveRadius : mBarHeight / 2;
        final int progressWidth = Math.max(0, width - radius * 2);
        final float progressPixels = mMax > 0 ? mProgress / (float) mMax * progressWidth : 0f;
        mKnobx = radius + (int) progressPixels;
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

        final int barHeight = isFocused() ? mActiveBarHeight : mBarHeight;

        final int width = getWidth();
        final int height = getHeight();
        final int verticalPadding = (height - barHeight) / 2;

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
