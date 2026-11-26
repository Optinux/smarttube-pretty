package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.vineyard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.leanback.widget.BaseCardView;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class IconCardView extends BaseCardView {
    RelativeLayout mLayout;
    RelativeLayout mHeaderLayout;
    View mDetailLayout;
    ImageView mIcon;
    TextView mTitle;
    TextView mValue;

    private int mSurfaceContainer;
    private int mSurfaceContainerHigh;
    private int mPrimaryContainer;
    private int mOnSurface;
    private int mOnSurfaceVariant;
    private int mOnPrimaryContainer;
    private int mOutlineColor;
    private int mHairlineColor;
    private float mCornerRadius;
    private int mFocusStrokePx;
    private int mHairlineStrokePx;

    public IconCardView(Context context, int styleResId) {
        super(new ContextThemeWrapper(context, styleResId), null, 0);
        buildCardView(styleResId);

    }

    public IconCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(getStyledContext(context, attrs, defStyleAttr), attrs, defStyleAttr);
        buildCardView(getImageCardViewStyle(context, attrs, defStyleAttr));
    }

    private void buildCardView(int styleResId) {
        // Make sure the ImageCardView is focusable.
        setFocusable(true);
        setFocusableInTouchMode(true);
        //setCardType(CARD_TYPE_INFO_UNDER);
        setCardType(CARD_TYPE_MAIN_ONLY);

        Context context = getContext();
        mSurfaceContainer = ViewUtil.getThemeColor(context, R.attr.m3SurfaceContainer, R.color.m3_grey_surface_container);
        mSurfaceContainerHigh = ViewUtil.getThemeColor(context, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        mPrimaryContainer = ViewUtil.getThemeColor(context, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        mOnSurface = ViewUtil.getThemeColor(context, R.attr.m3OnSurface, R.color.m3_on_surface);
        mOnSurfaceVariant = ViewUtil.getThemeColor(context, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
        mOnPrimaryContainer = ViewUtil.getThemeColor(context, R.attr.m3OnPrimaryContainer, R.color.m3_on_surface);
        mOutlineColor = ViewUtil.getThemeColor(context, R.attr.m3Outline, R.color.m3_grey_outline);
        mHairlineColor = ViewUtil.getThemeColor(context, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        mCornerRadius = getResources().getDimension(R.dimen.m3_card_corner_radius);
        mFocusStrokePx = getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);
        mHairlineStrokePx = getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        setBackground(ViewUtil.createRoundedDrawable(mSurfaceContainer, 0, 0, mCornerRadius));
        ViewUtil.applyRoundedOutline(this, mCornerRadius);

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_options_item, this);

        mLayout = view.findViewById(R.id.layout_option_card);
        mHeaderLayout = view.findViewById(R.id.layout_header);
        mDetailLayout = view.findViewById(R.id.layout_detail);
        mIcon = view.findViewById(R.id.image_option);
        mTitle = view.findViewById(R.id.text_option_title);
        mValue = view.findViewById(R.id.text_option_value);

        if (mHeaderLayout != null) {
            mHeaderLayout.setBackgroundColor(mSurfaceContainerHigh);
        }
        updateSelectedAppearance(false);

        TypedArray cardAttrs =
                getContext().obtainStyledAttributes(
                        styleResId, R.styleable.lbImageCardView);
        cardAttrs.recycle();
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        updateSelectedAppearance(selected);
    }

    private void updateSelectedAppearance(boolean selected) {
        int fromBg = selected ? mSurfaceContainer : mPrimaryContainer;
        int toBg = selected ? mPrimaryContainer : mSurfaceContainer;
        int fromStroke = selected ? mHairlineColor : mOutlineColor;
        int toStroke = selected ? mOutlineColor : mHairlineColor;
        int fromStrokePx = selected ? mHairlineStrokePx : mFocusStrokePx;
        int toStrokePx = selected ? mFocusStrokePx : mHairlineStrokePx;
        int fromTitle = selected ? mOnSurface : mOnPrimaryContainer;
        int toTitle = selected ? mOnPrimaryContainer : mOnSurface;
        int fromValue = selected ? mOnSurfaceVariant : mOnPrimaryContainer;
        int toValue = selected ? mOnPrimaryContainer : mOnSurfaceVariant;

        M3Motion.applyCardFocusTransition(
                this,
                selected,
                fromBg,
                toBg,
                fromStroke,
                toStroke,
                fromStrokePx,
                toStrokePx,
                mCornerRadius,
                (fraction, currentFill) -> {
                    if (mDetailLayout != null) {
                        mDetailLayout.setBackgroundColor(currentFill);
                    }
                    if (mTitle != null) {
                        mTitle.setTextColor(ColorUtils.blendARGB(fromTitle, toTitle, fraction));
                    }
                    if (mValue != null) {
                        mValue.setTextColor(ColorUtils.blendARGB(fromValue, toValue, fraction));
                    }
                });
    }

    public void setMainImageDimensions(int width, int height) {
        ViewGroup.LayoutParams lp = mLayout.getLayoutParams();
        lp.width = width;
        lp.height = height;
        mLayout.setLayoutParams(lp);
    }

    public void setOptionIcon(Drawable drawable) {
        mIcon.setImageDrawable(drawable);
    }

    public void setOptionTitleText(String titleText) {
        mTitle.setText(titleText);
    }

    public void setOptionValueText(String valueText) {
        mValue.setText(valueText);
    }

    private static Context getStyledContext(Context context, AttributeSet attrs, int defStyleAttr) {
        int style = getImageCardViewStyle(context, attrs, defStyleAttr);
        return new ContextThemeWrapper(context, style);
    }

    private static int getImageCardViewStyle(Context context, AttributeSet attrs, int defStyleAttr) {
        // Read style attribute defined in XML layout.
        int style = null == attrs ? 0 : attrs.getStyleAttribute();
        if (0 == style) {
            // Not found? Read global ImageCardView style from Theme attribute.
            TypedArray styledAttrs =
                    context.obtainStyledAttributes(
                            R.styleable.LeanbackTheme);
            style = styledAttrs.getResourceId(
                    R.styleable.LeanbackTheme_imageCardViewStyle, 0);
            styledAttrs.recycle();
        }
        return style;
    }

    public IconCardView(Context context) {
        this(context, null);
    }

    public IconCardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.imageCardViewStyle);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

}
