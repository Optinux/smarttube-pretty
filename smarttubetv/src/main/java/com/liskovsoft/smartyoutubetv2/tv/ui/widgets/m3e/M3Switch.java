package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * Custom Material 3 Expressive Switch implementing M3 track (52x32dp) and morphing handle
 * (16dp unchecked -> 24dp checked with check glyph) driven by M3 Fast Spatial spring motion.
 */
public class M3Switch extends View {
    private static final float TRACK_WIDTH_DP = 52f;
    private static final float TRACK_HEIGHT_DP = 32f;
    private static final float HANDLE_UNCHECKED_DP = 16f;
    private static final float HANDLE_CHECKED_DP = 24f;
    private static final float TRACK_STROKE_DP = 2f;

    private final Paint mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHandlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCheckPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mTrackRect = new RectF();

    private boolean mChecked;
    private float mProgress;
    private ValueAnimator mAnimator;

    private int mTrackUnchecked;
    private int mTrackChecked;
    private int mOutlineColor;
    private int mHandleUnchecked;
    private int mHandleChecked;
    private int mCheckIconColor;

    public M3Switch(Context context) {
        this(context, null);
    }

    public M3Switch(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public M3Switch(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mCheckPaint.setStyle(Paint.Style.STROKE);
        mCheckPaint.setStrokeCap(Paint.Cap.ROUND);
        mCheckPaint.setStrokeJoin(Paint.Join.ROUND);
        refreshThemeColors();
    }

    public void refreshThemeColors() {
        Context ctx = getContext();
        mTrackUnchecked = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerHighest, R.color.m3_grey_surface_container_highest);
        mTrackChecked = ViewUtil.getThemeColor(ctx, R.attr.m3Primary, R.color.m3_grey_primary);
        mOutlineColor = ViewUtil.getThemeColor(ctx, R.attr.m3Outline, R.color.m3_grey_outline);
        mHandleUnchecked = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
        mHandleChecked = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimary, R.color.m3_grey_on_primary);
        mCheckIconColor = ViewUtil.getThemeColor(ctx, R.attr.m3Primary, R.color.m3_grey_primary);
        invalidate();
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked, boolean animate) {
        if (mChecked == checked && (animate || Math.abs(mProgress - (checked ? 1f : 0f)) < 0.01f)) {
            return;
        }
        mChecked = checked;
        float target = checked ? 1f : 0f;
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        if (!animate || !M3Motion.areAnimationsEnabled(getContext()) || !ViewCompat.isAttachedToWindow(this)) {
            mProgress = target;
            invalidate();
            return;
        }
        mAnimator = ValueAnimator.ofFloat(mProgress, target);
        mAnimator.setDuration(M3Motion.scaledDuration(M3Motion.DURATION_FAST_MS, target - mProgress));
        mAnimator.setInterpolator(M3Motion.standard());
        mAnimator.addUpdateListener(anim -> {
            mProgress = (float) anim.getAnimatedValue();
            invalidate();
        });
        mAnimator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int desiredW = Math.round(TRACK_WIDTH_DP * density);
        int desiredH = Math.round(TRACK_HEIGHT_DP * density);
        setMeasuredDimension(
                resolveSize(desiredW, widthMeasureSpec),
                resolveSize(desiredH, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float w = getWidth();
        float h = getHeight();
        float radius = h / 2f;
        float strokePx = TRACK_STROKE_DP * density;

        float clamped = Math.max(0f, Math.min(1f, mProgress));
        mTrackPaint.setColor(ColorUtils.blendARGB(mTrackUnchecked, mTrackChecked, clamped));
        mTrackRect.set(0f, 0f, w, h);
        canvas.drawRoundRect(mTrackRect, radius, radius, mTrackPaint);

        if (clamped < 0.99f) {
            int strokeAlpha = Math.round(255 * (1f - clamped));
            mStrokePaint.setColor(ColorUtils.setAlphaComponent(mOutlineColor, strokeAlpha));
            mStrokePaint.setStrokeWidth(strokePx);
            float half = strokePx / 2f;
            mTrackRect.set(half, half, w - half, h - half);
            canvas.drawRoundRect(mTrackRect, radius - half, radius - half, mStrokePaint);
        }

        float handleDiameter = (HANDLE_UNCHECKED_DP + (HANDLE_CHECKED_DP - HANDLE_UNCHECKED_DP) * clamped) * density;
        float handleRadius = handleDiameter / 2f;
        float marginStart = (TRACK_HEIGHT_DP - HANDLE_UNCHECKED_DP) * 0.5f * density;
        float marginEnd = (TRACK_HEIGHT_DP - HANDLE_CHECKED_DP) * 0.5f * density;
        float startCx = marginStart + (HANDLE_UNCHECKED_DP * 0.5f * density);
        float endCx = w - marginEnd - (HANDLE_CHECKED_DP * 0.5f * density);
        float cx = startCx + (endCx - startCx) * clamped;
        float cy = h / 2f;

        mHandlePaint.setColor(ColorUtils.blendARGB(mHandleUnchecked, mHandleChecked, clamped));
        canvas.drawCircle(cx, cy, handleRadius, mHandlePaint);

        if (clamped > 0.35f) {
            float alphaFactor = Math.min(1f, (clamped - 0.35f) / 0.65f);
            mCheckPaint.setColor(ColorUtils.setAlphaComponent(mCheckIconColor, Math.round(255 * alphaFactor)));
            mCheckPaint.setStrokeWidth(2.1f * density);
            float s = handleRadius * 0.48f;
            canvas.drawLine(cx - s * 0.95f, cy + s * 0.05f, cx - s * 0.15f, cy + s * 0.85f, mCheckPaint);
            canvas.drawLine(cx - s * 0.15f, cy + s * 0.85f, cx + s * 1.0f, cy - s * 0.65f, mCheckPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        super.onDetachedFromWindow();
    }
}
