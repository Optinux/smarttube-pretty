package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.ImageViewCompat;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowHeaderPresenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class IconHeaderItemPresenter extends RowHeaderPresenter {
    private static final String TAG = IconHeaderItemPresenter.class.getSimpleName();
    private final int mResId;
    private final String mIconUrl;
    private Drawable mDefaultIcon;
    private int mSecondaryContainerColor;
    private int mOnSecondaryContainerColor;
    private int mOnSurfaceVariantColor;
    private int mOutlineColor;
    private int mPrimaryColor;
    private float mPillCornerRadius;
    private int mFocusStrokeWidthPx;
    private boolean mThemeResolved;

    public IconHeaderItemPresenter(int resId, String iconUrl) {
        mResId = resId;
        mIconUrl = iconUrl;
    }

    private void ensureThemeTokens(Context context) {
        if (mThemeResolved || context == null) {
            return;
        }
        mSecondaryContainerColor = ViewUtil.getThemeColor(context, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container);
        mOnSecondaryContainerColor = ViewUtil.getThemeColor(context, R.attr.m3OnSecondaryContainer, R.color.m3_grey_on_secondary_container);
        mOnSurfaceVariantColor = ViewUtil.getThemeColor(context, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
        mOutlineColor = ViewUtil.getThemeColor(context, R.attr.m3Outline, R.color.m3_grey_outline);
        mPrimaryColor = ViewUtil.getThemeColor(context, R.attr.m3Primary, R.color.m3_grey_primary);
        mPillCornerRadius = context.getResources().getDimension(R.dimen.m3_pill_corner_radius);
        mFocusStrokeWidthPx = Math.max(1, Math.round(1.5f * context.getResources().getDisplayMetrics().density));
        mThemeResolved = true;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        Context context = viewGroup.getContext();
        ensureThemeTokens(context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDefaultIcon = new ColorDrawable(Color.TRANSPARENT);

        View view = inflater.inflate(R.layout.icon_header_item, viewGroup, false);
        view.setBackground(ViewUtil.createRoundedDrawable(Color.TRANSPARENT, Color.TRANSPARENT, 0, mPillCornerRadius));

        ViewHolder holder = new ViewHolder(view);
        applySelectLevel(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        HeaderItem headerItem;

        if (item instanceof PageRow) {
            headerItem = ((PageRow) item).getHeaderItem();
        } else {
            headerItem = ((ListRow) item).getHeaderItem();
        }

        View rootView = viewHolder.view;
        rootView.setFocusable(true);

        ImageView iconView = rootView.findViewById(R.id.header_icon);
        if (iconView != null) {
            if (mIconUrl != null) {
                ImageViewCompat.setImageTintList(iconView, null);
                iconView.clearColorFilter();
                Glide.with(rootView.getContext())
                        .load(mIconUrl)
                        .apply(ViewUtil.glideOptions().circleCrop().error(mDefaultIcon))
                        .listener(mErrorListener)
                        .transition(ViewUtil.glideCrossFade(rootView.getContext()))
                        .into(iconView);

                //ViewUtil.makeMonochrome(iconView);
            } else {
                Drawable icon = mResId > 0 ? ContextCompat.getDrawable(rootView.getContext(), mResId) : mDefaultIcon;
                iconView.setImageDrawable(icon);
            }
        }

        TextView label = rootView.findViewById(R.id.header_label);
        if (label != null) {
            label.setText(headerItem.getName());
        }

        if (viewHolder instanceof RowHeaderPresenter.ViewHolder) {
            applySelectLevel((RowHeaderPresenter.ViewHolder) viewHolder);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        if (viewHolder != null && viewHolder.view != null) {
            M3Motion.resetOnUnbind(viewHolder.view);
            ImageView iconView = viewHolder.view.findViewById(R.id.header_icon);
            if (iconView != null) {
                Glide.with(viewHolder.view.getContext()).clear(iconView);
                iconView.setScaleX(1.0f);
                iconView.setScaleY(1.0f);
            }
        }
    }

    // TODO: This is a temporary fix. Remove me when leanback onCreateViewHolder no longer sets the
    // mUnselectAlpha, and also assumes the xml inflation will return a RowHeaderView.
    @Override
    protected void onSelectLevelChanged(RowHeaderPresenter.ViewHolder holder) {
        applySelectLevel(holder);
    }

    private void applySelectLevel(RowHeaderPresenter.ViewHolder holder) {
        if (holder == null || holder.view == null) {
            return;
        }
        ensureThemeTokens(holder.view.getContext());

        float level = Math.max(0f, Math.min(1f, holder.getSelectLevel()));
        holder.view.setAlpha(1.0f);

        Drawable bg = holder.view.getBackground();
        if (bg instanceof GradientDrawable) {
            GradientDrawable pill = (GradientDrawable) bg;
            int fillAlpha = Math.round(Color.alpha(mSecondaryContainerColor) * level);
            pill.setColor(ColorUtils.setAlphaComponent(mSecondaryContainerColor, fillAlpha));
            int strokeAlpha = Math.round(255 * level);
            pill.setStroke(strokeAlpha > 0 ? mFocusStrokeWidthPx : 0,
                    ColorUtils.setAlphaComponent(mOutlineColor, strokeAlpha));
        }

        TextView label = holder.view.findViewById(R.id.header_label);
        if (label != null) {
            label.setTextColor(ColorUtils.blendARGB(mOnSurfaceVariantColor, mOnSecondaryContainerColor, level));
            label.setAlpha(1.0f);
            label.setTypeface(Typeface.create("sans-serif-medium", level > 0.5f ? Typeface.BOLD : Typeface.NORMAL));
        }

        ImageView iconView = holder.view.findViewById(R.id.header_icon);
        if (iconView != null) {
            iconView.setAlpha(1.0f);
            float iconScale = 1.0f + 0.10f * level;
            iconView.setScaleX(iconScale);
            iconView.setScaleY(iconScale);
            if (mIconUrl == null && mResId > 0) {
                int iconTint = ColorUtils.blendARGB(mOnSurfaceVariantColor, mPrimaryColor, level);
                ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(iconTint));
            } else {
                ImageViewCompat.setImageTintList(iconView, null);
            }
        }
    }

    private final RequestListener<Drawable> mErrorListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
}
