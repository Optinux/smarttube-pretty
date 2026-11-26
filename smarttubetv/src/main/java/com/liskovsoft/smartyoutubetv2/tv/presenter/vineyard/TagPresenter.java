package com.liskovsoft.smartyoutubetv2.tv.presenter.vineyard;

import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;
import androidx.core.graphics.ColorUtils;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.User;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.vineyard.TagCardView;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
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
        mFocusStrokePx = context.getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);
        mHairlineStrokePx = 0;

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
        int fromBg = selected ? mDefaultBackgroundColor : mSelectedBackgroundColor;
        int toBg = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;
        int fromStroke = selected ? 0 : mOutlineColor;
        int toStroke = selected ? mOutlineColor : 0;
        int fromStrokePx = selected ? 0 : mFocusStrokePx;
        int toStrokePx = selected ? mFocusStrokePx : 0;
        int fromText = selected ? mDefaultTextColor : mSelectedTextColor;
        int toText = selected ? mSelectedTextColor : mDefaultTextColor;

        M3Motion.applyCardFocusTransition(
                view,
                selected,
                fromBg,
                toBg,
                fromStroke,
                toStroke,
                fromStrokePx,
                toStrokePx,
                mPillRadius,
                (fraction, currentFill) -> view.setTextColor(ColorUtils.blendARGB(fromText, toText, fraction)));
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
        M3Motion.cancelFocusAnimations(viewHolder.view);
    }

}