package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.vineyard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

import androidx.leanback.widget.BaseCardView;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class LoadingCardView extends BaseCardView {

    private ProgressBar mProgressBar;

    public LoadingCardView(Context context, int styleResId) {
        super(new ContextThemeWrapper(context, styleResId), null, 0);
        buildLoadingCardView(styleResId);
    }

    public LoadingCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(getStyledContext(context, attrs, defStyleAttr), attrs, defStyleAttr);
        buildLoadingCardView(getImageCardViewStyle(context, attrs, defStyleAttr));
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private void buildLoadingCardView(int styleResId) {
        setFocusable(false);
        setFocusableInTouchMode(false);
        setCardType(CARD_TYPE_MAIN_ONLY);
        Context context = getContext();
        int surfaceColor = ViewUtil.getThemeColor(
                context, R.attr.m3SurfaceContainer, R.color.m3_grey_surface_container);
        float cornerRadius = getResources().getDimension(R.dimen.m3_card_corner_radius);
        setBackground(ViewUtil.createRoundedDrawable(surfaceColor, 0, 0, cornerRadius));
        ViewUtil.applyRoundedOutline(this, cornerRadius);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.view_loading_card, this);
        TypedArray cardAttrs =
                getContext().obtainStyledAttributes(
                        styleResId, R.styleable.lbImageCardView);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_indicator);
        if (mProgressBar != null) {
            int primaryColor = ViewUtil.getThemeColor(
                    context, R.attr.m3Primary, R.color.m3_grey_primary);
            if (Build.VERSION.SDK_INT >= 21) {
                mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(primaryColor));
            } else if (mProgressBar.getIndeterminateDrawable() != null) {
                mProgressBar.getIndeterminateDrawable().setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN);
            }
        }
        cardAttrs.recycle();
    }

    public void isLoading(boolean isLoading) {
        mProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private static Context getStyledContext(Context context, AttributeSet attrs, int defStyleAttr) {
        int style = getImageCardViewStyle(context, attrs, defStyleAttr);
        return new ContextThemeWrapper(context, style);
    }

    private static int getImageCardViewStyle(Context context, AttributeSet attrs, int defStyleAttr) {
        int style = null == attrs ? 0 : attrs.getStyleAttribute();
        if (0 == style) {
            TypedArray styledAttrs =
                    context.obtainStyledAttributes(
                            R.styleable.LeanbackTheme);
            style = styledAttrs.getResourceId(
                            R.styleable.LeanbackTheme_imageCardViewStyle, 0);
            styledAttrs.recycle();
        }
        return style;
    }

    public LoadingCardView(Context context) {
        this(context, null);
    }

    public LoadingCardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.imageCardViewStyle);
    }

}