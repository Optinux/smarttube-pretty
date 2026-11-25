package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class SettingsCardPresenter extends Presenter {
    private int mDefaultBackgroundColor;
    private int mDefaultTextColor;
    private int mSelectedBackgroundColor;
    private int mSelectedTextColor;
    private int mOutlineColor;
    private int mHairlineColor;
    private float mCornerRadius;
    private int mFocusStrokePx;
    private int mHairlineStrokePx;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();

        mDefaultBackgroundColor =
                ViewUtil.getThemeColor(context, R.attr.m3SurfaceContainer, R.color.card_default_background);
        mSelectedBackgroundColor =
                ViewUtil.getThemeColor(context, R.attr.m3PrimaryContainer, R.color.card_selected_background_white);
        mDefaultTextColor =
                ViewUtil.getThemeColor(context, R.attr.m3OnSurface, R.color.m3_on_surface);
        mSelectedTextColor =
                ViewUtil.getThemeColor(context, R.attr.m3OnPrimaryContainer, R.color.m3_on_surface);
        mOutlineColor =
                ViewUtil.getThemeColor(context, R.attr.m3Outline, R.color.m3_grey_outline);
        mHairlineColor =
                ViewUtil.getThemeColor(context, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);

        mCornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 18f, context.getResources().getDisplayMetrics());
        mFocusStrokePx = context.getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);
        mHairlineStrokePx = context.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        @SuppressLint("InflateParams")
        FrameLayout container = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.settings_card, null);
        ViewUtil.applyRoundedOutline(container, mCornerRadius);

        TextView textView = container.findViewById(R.id.settings_title);
        ImageView imageView = container.findViewById(R.id.settings_image);
        updateCardStyle(container, textView, imageView, false);

        ViewUtil.setTextScrollSpeed(textView, getCardTextScrollSpeed(context));

        container.setOnFocusChangeListener((v, hasFocus) -> {
            updateCardStyle(container, textView, imageView, hasFocus);

            if (hasFocus) {
                ViewUtil.enableMarquee(textView);
            } else {
                ViewUtil.disableMarquee(textView);
            }
        });

        return new ViewHolder(container);
    }

    private void updateCardStyle(FrameLayout container, TextView textView, ImageView imageView, boolean hasFocus) {
        int backgroundColor = hasFocus ? mSelectedBackgroundColor : mDefaultBackgroundColor;
        int contentColor = hasFocus ? mSelectedTextColor : mDefaultTextColor;

        container.setBackground(ViewUtil.createRoundedDrawable(backgroundColor, 0, 0, mCornerRadius));
        ViewUtil.updateCardFocusForeground(
                container, hasFocus, mOutlineColor, mHairlineColor, mFocusStrokePx, mHairlineStrokePx, mCornerRadius);
        textView.setTextColor(contentColor);
        if (imageView != null) {
            imageView.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        SettingsItem settingsItem = (SettingsItem) item;

        TextView textView = viewHolder.view.findViewById(R.id.settings_title);

        textView.setText(settingsItem.title);

        if (settingsItem.imageResId > 0) {
            Context context = viewHolder.view.getContext();
            ImageView imageView = viewHolder.view.findViewById(R.id.settings_image);
            imageView.setImageDrawable(ContextCompat.getDrawable(context, settingsItem.imageResId));
            imageView.setColorFilter(viewHolder.view.hasFocus() ? mSelectedTextColor : mDefaultTextColor, PorterDuff.Mode.SRC_IN);
            imageView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }

    protected boolean isCardTextAutoScrollEnabled(Context context) {
        return MainUIData.instance(context).isCardTextAutoScrollEnabled();
    }

    protected float getCardTextScrollSpeed(Context context) {
        return MainUIData.instance(context).getCardTextScrollSpeed();
    }
}
