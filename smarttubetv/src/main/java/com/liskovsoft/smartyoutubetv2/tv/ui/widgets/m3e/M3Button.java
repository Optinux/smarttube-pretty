package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * Material 3 Expressive button for Android TV with shape morphing (pill to rounded square on focus/press),
 * tonal/filled/outlined emphasis styles, leading icon support, and M3 spring motion physics.
 */
public class M3Button extends LinearLayout {
    public static final int STYLE_FILLED = 0;
    public static final int STYLE_TONAL = 1;
    public static final int STYLE_OUTLINED = 2;

    private int mStyle = STYLE_FILLED;
    private ImageView mIconView;
    private TextView mLabelView;
    private float mRestingRadiusPx;
    private float mMorphedRadiusPx;
    private float mCurrentRadiusPx;
    private int mFocusStrokePx;
    private int mHairlineStrokePx;

    private int mRestingFill;
    private int mFocusedFill;
    private int mRestingContent;
    private int mFocusedContent;
    private int mRestingStroke;
    private int mFocusedStroke;
    private OnFocusChangeListener mExtraFocusListener;

    public M3Button(Context context) {
        this(context, null);
    }

    public M3Button(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public M3Button(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);

        float density = context.getResources().getDisplayMetrics().density;
        int minHeight = Math.round(44 * density);
        int padH = Math.round(18 * density);
        int padV = Math.round(10 * density);
        setMinimumHeight(minHeight);
        setPaddingRelative(padH, padV, padH, padV);

        mRestingRadiusPx = context.getResources().getDimension(R.dimen.m3_pill_corner_radius);
        mMorphedRadiusPx = 14f * density;
        mCurrentRadiusPx = mRestingRadiusPx;
        mFocusStrokePx = context.getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);
        mHairlineStrokePx = context.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        mIconView = new ImageView(context);
        int iconSize = Math.round(20 * density);
        LayoutParams iconLp = new LayoutParams(iconSize, iconSize);
        iconLp.setMarginEnd(Math.round(8 * density));
        mIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mIconView.setVisibility(GONE);
        addView(mIconView, iconLp);

        mLabelView = new TextView(context);
        mLabelView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mLabelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mLabelView.setSingleLine(true);
        mLabelView.setEllipsize(TextUtils.TruncateAt.END);
        mLabelView.setIncludeFontPadding(false);
        addView(mLabelView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        resolveThemeColors();
        applyVisualState(false, false);

        setOnFocusChangeListener((v, hasFocus) -> {
            applyVisualState(hasFocus, true);
            M3Motion.animateFocusScale(this, hasFocus, 1.045f);
            if (mExtraFocusListener != null) {
                mExtraFocusListener.onFocusChange(v, hasFocus);
            }
        });
    }

    public void addOnFocusChangeListener(@Nullable OnFocusChangeListener listener) {
        mExtraFocusListener = listener;
    }

    public void setButtonStyle(int style) {
        if (mStyle != style) {
            mStyle = style;
            resolveThemeColors();
            applyVisualState(hasFocus(), false);
        }
    }

    public void setText(@Nullable CharSequence text) {
        mLabelView.setText(text);
        setContentDescription(text);
    }

    public void setIcon(@DrawableRes int resId) {
        if (resId > 0) {
            mIconView.setImageDrawable(ContextCompat.getDrawable(getContext(), resId));
            mIconView.setVisibility(VISIBLE);
            M3Motion.setImageTint(mIconView, hasFocus() ? mFocusedContent : mRestingContent);
        } else {
            mIconView.setImageDrawable(null);
            mIconView.setVisibility(GONE);
        }
    }

    /**
     * Configures this M3Button as a 48dp square icon-only button reusing the exact M3 focus highlight and morph.
     */
    public void setIconOnly(@DrawableRes int resId) {
        float density = getContext().getResources().getDisplayMetrics().density;
        int targetSize = Math.round(48 * density);
        int iconSize = Math.round(22 * density);
        setMinimumWidth(targetSize);
        setMinimumHeight(targetSize);
        setGravity(Gravity.CENTER);
        setPaddingRelative(0, 0, 0, 0);
        mLabelView.setVisibility(GONE);
        LayoutParams iconLp = (LayoutParams) mIconView.getLayoutParams();
        iconLp.width = iconSize;
        iconLp.height = iconSize;
        iconLp.setMarginEnd(0);
        mIconView.setLayoutParams(iconLp);
        mRestingRadiusPx = 24f * density;
        mCurrentRadiusPx = hasFocus() ? mMorphedRadiusPx : mRestingRadiusPx;
        setIcon(resId);
        applyVisualState(hasFocus(), false);
    }

    public void refreshThemeColors() {
        resolveThemeColors();
        applyVisualState(hasFocus(), false);
    }

    private void resolveThemeColors() {
        Context ctx = getContext();
        int primary = ViewUtil.getThemeColor(ctx, R.attr.m3Primary, R.color.m3_grey_primary);
        int onPrimary = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimary, R.color.m3_grey_on_primary);
        int primaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        int onPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);
        int secondaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container);
        int onSecondaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnSecondaryContainer, R.color.m3_grey_on_secondary_container);
        int surfaceHigh = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
        int outline = ViewUtil.getThemeColor(ctx, R.attr.m3Outline, R.color.m3_grey_outline);
        int outlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);

        switch (mStyle) {
            case STYLE_TONAL:
                mRestingFill = secondaryContainer;
                mFocusedFill = primaryContainer;
                mRestingContent = onSecondaryContainer;
                mFocusedContent = onPrimaryContainer;
                mRestingStroke = Color.TRANSPARENT;
                mFocusedStroke = outline;
                break;
            case STYLE_OUTLINED:
                mRestingFill = ColorUtils.setAlphaComponent(surfaceHigh, 190);
                mFocusedFill = secondaryContainer;
                mRestingContent = onSurface;
                mFocusedContent = onSecondaryContainer;
                mRestingStroke = outlineVariant;
                mFocusedStroke = outline;
                break;
            case STYLE_FILLED:
            default:
                mRestingFill = primaryContainer;
                mFocusedFill = primary;
                mRestingContent = onPrimaryContainer;
                mFocusedContent = onPrimary;
                mRestingStroke = Color.TRANSPARENT;
                mFocusedStroke = outline;
                break;
        }
    }

    private void applyVisualState(boolean focused, boolean animate) {
        if (!animate) {
            M3Motion.cancelFocusAnimations(this);
        }

        int fromFill = focused ? mRestingFill : mFocusedFill;
        int toFill = focused ? mFocusedFill : mRestingFill;
        int fromStroke = focused ? mRestingStroke : mFocusedStroke;
        int toStroke = focused ? mFocusedStroke : mRestingStroke;
        int fromStrokePx = focused ? (mStyle == STYLE_OUTLINED ? mHairlineStrokePx : 0) : mFocusStrokePx;
        int toStrokePx = focused ? mFocusStrokePx : (mStyle == STYLE_OUTLINED ? mHairlineStrokePx : 0);
        int fromContent = focused ? mRestingContent : mFocusedContent;
        int toContent = focused ? mFocusedContent : mRestingContent;

        float targetRadius = focused ? mMorphedRadiusPx : mRestingRadiusPx;
        if (getBackground() instanceof GradientDrawable && animate) {
            M3Motion.animateCornerMorph(this, (GradientDrawable) getBackground(), mCurrentRadiusPx, targetRadius);
        }
        mCurrentRadiusPx = targetRadius;

        M3Motion.applyCardFocusTransition(
                this,
                focused,
                fromFill,
                toFill,
                fromStroke,
                toStroke,
                fromStrokePx,
                toStrokePx,
                targetRadius,
                (fraction, currentFill) -> {
                    int blended = ColorUtils.blendARGB(fromContent, toContent, fraction);
                    mLabelView.setTextColor(blended);
                    if (mIconView.getVisibility() == VISIBLE) {
                        M3Motion.setImageTint(mIconView, blended);
                    }
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        M3Motion.cancelFocusAnimations(this);
        super.onDetachedFromWindow();
    }
}
