package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.time.DateTimeView;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * Material 3 Expressive clock pill component for the home screen header.
 * Displays time and date inside a tonal pill container with an optically concentric icon badge.
 */
public class M3ClockView extends LinearLayout {
    private FrameLayout mIconBadge;
    private ImageView mIconView;
    private DateTimeView mTimeView;
    private DateTimeView mDateView;

    public M3ClockView(Context context) {
        this(context, null);
    }

    public M3ClockView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public M3ClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setFocusable(false);
        setFocusableInTouchMode(false);
        setClickable(false);

        float density = context.getResources().getDisplayMetrics().density;
        int minHeight = Math.round(44 * density);
        int padStart = Math.round(6 * density);
        int padEnd = Math.round(14 * density);
        int padV = Math.round(6 * density);
        setMinimumHeight(minHeight);
        setPaddingRelative(padStart, padV, padEnd, padV);

        int badgeSize = Math.round(32 * density);
        int iconSize = Math.round(16 * density);
        mIconBadge = new FrameLayout(context);
        mIconBadge.setFocusable(false);
        mIconBadge.setClickable(false);
        LayoutParams badgeLp = new LayoutParams(badgeSize, badgeSize);
        badgeLp.setMarginEnd(Math.round(10 * density));

        mIconView = new ImageView(context);
        mIconView.setFocusable(false);
        mIconView.setClickable(false);
        mIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mIconView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_m3_clock));
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER);
        mIconBadge.addView(mIconView, iconLp);
        addView(mIconBadge, badgeLp);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(VERTICAL);
        textCol.setGravity(Gravity.CENTER_VERTICAL);
        textCol.setFocusable(false);
        textCol.setClickable(false);

        mTimeView = new DateTimeView(context);
        mTimeView.setId(R.id.global_time);
        mTimeView.showDate(false);
        mTimeView.showTime(true);
        mTimeView.setFocusable(false);
        mTimeView.setClickable(false);
        mTimeView.setSingleLine(true);
        mTimeView.setIncludeFontPadding(false);
        mTimeView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mTimeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        if (Build.VERSION.SDK_INT >= 21) {
            mTimeView.setFontFeatureSettings("tnum");
        }
        textCol.addView(mTimeView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        mDateView = new DateTimeView(context);
        mDateView.setId(R.id.global_date);
        mDateView.showTime(false);
        mDateView.showDate(true);
        mDateView.setFocusable(false);
        mDateView.setClickable(false);
        mDateView.setSingleLine(true);
        mDateView.setIncludeFontPadding(false);
        mDateView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        if (Build.VERSION.SDK_INT >= 21) {
            mDateView.setFontFeatureSettings("tnum");
        }
        LayoutParams dateLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        dateLp.topMargin = Math.round(1 * density);
        textCol.addView(mDateView, dateLp);

        addView(textCol, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        syncChildVisibility(getVisibility());
        refreshThemeColors();
    }

    public void refreshThemeColors() {
        Context ctx = getContext();
        int surfaceContainerHigh = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int secondaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container);
        int onSecondaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnSecondaryContainer, R.color.m3_grey_on_secondary_container);
        int outlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
        int onSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);

        float pillRadius = getResources().getDimension(R.dimen.m3_pill_corner_radius);
        int hairlinePx = getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        setBackground(ViewUtil.createRoundedDrawable(surfaceContainerHigh, outlineVariant, hairlinePx, pillRadius));
        mIconBadge.setBackground(ViewUtil.createRoundedDrawable(secondaryContainer, 0, 0, pillRadius));
        M3Motion.setImageTint(mIconView, onSecondaryContainer);
        mTimeView.setTextColor(onSurface);
        mDateView.setTextColor(onSurfaceVariant);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        syncChildVisibility(visibility);
    }

    private void syncChildVisibility(int visibility) {
        if (mTimeView != null) {
            mTimeView.setVisibility(visibility);
        }
        if (mDateView != null) {
            mDateView.setVisibility(visibility);
        }
    }

    public DateTimeView getTimeView() {
        return mTimeView;
    }

    public DateTimeView getDateView() {
        return mDateView;
    }
}
