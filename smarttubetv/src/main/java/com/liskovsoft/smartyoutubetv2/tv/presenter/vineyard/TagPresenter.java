package com.liskovsoft.smartyoutubetv2.tv.presenter.vineyard;

import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.User;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.vineyard.TagCardView;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class TagPresenter extends LongClickPresenter {
    private int mDefaultBackgroundColor;
    private int mDefaultTextColor;
    private int mSelectedBackgroundColor;
    private int mSelectedTextColor;
    private int mOutlineColor;
    private int mHairlineColor;
    private float mPillRadius;
    private int mFocusStrokePx;
    private int mHairlineStrokePx;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();

        mDefaultBackgroundColor =
                ViewUtil.getThemeColor(context, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        mSelectedBackgroundColor =
                ViewUtil.getThemeColor(context, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        mDefaultTextColor =
                ViewUtil.getThemeColor(context, R.attr.m3OnSurface, R.color.m3_on_surface);
        mSelectedTextColor =
                ViewUtil.getThemeColor(context, R.attr.m3OnPrimaryContainer, R.color.m3_on_surface);
        mOutlineColor =
                ViewUtil.getThemeColor(context, R.attr.m3Outline, R.color.m3_grey_outline);
        mHairlineColor =
                ViewUtil.getThemeColor(context, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);

        mPillRadius = context.getResources().getDimension(R.dimen.m3_pill_corner_radius);
        mFocusStrokePx = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2f, context.getResources().getDisplayMetrics()));
        mHairlineStrokePx = context.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        TagCardView cardView = new TagCardView(context) {
            @Override
            public void setSelected(boolean selected) {
                updateCardAppearance(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        ViewUtil.applyRoundedOutline(cardView, mPillRadius);
        updateCardAppearance(cardView, false);
        return new ViewHolder(cardView);
    }

    private void updateCardAppearance(TagCardView view, boolean selected) {
        int bgColor = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;
        int textColor = selected ? mSelectedTextColor : mDefaultTextColor;
        view.setBackground(ViewUtil.createRoundedDrawable(bgColor, 0, 0, mPillRadius));
        ViewUtil.updateCardFocusForeground(
                view, selected, mOutlineColor, mHairlineColor, mFocusStrokePx, mHairlineStrokePx, mPillRadius);
        view.setTextColor(textColor);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);

        if (item instanceof Tag) {
            Tag post = (Tag) item;
            TagCardView cardView = (TagCardView) viewHolder.view;

            if (post.tag != null) {
                cardView.setCardText(post.tag);
                //cardView.setCardIcon(R.drawable.ic_tag);
            }
        } else if (item instanceof User) {
            User post = (User) item;
            TagCardView cardView = (TagCardView) viewHolder.view;

            if (post.username != null) {
                cardView.setCardText(post.username);
                cardView.setCardIcon(R.drawable.ic_user);
            }
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }

}