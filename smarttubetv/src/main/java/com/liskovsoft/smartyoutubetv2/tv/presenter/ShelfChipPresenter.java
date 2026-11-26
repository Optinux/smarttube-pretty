package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.leanback.widget.RowPresenter;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e.M3ChipBarView;

/**
 * Renders an M3 Expressive horizontal quick-jump chip strip inside MultipleRowsFragment.
 */
public class ShelfChipPresenter extends RowPresenter {
    public ShelfChipPresenter() {
        setSelectEffectEnabled(false);
    }

    @Override
    protected ViewHolder createRowViewHolder(ViewGroup parent) {
        Context ctx = parent.getContext();
        float density = ctx.getResources().getDisplayMetrics().density;

        FrameLayout wrapper = new FrameLayout(ctx);
        wrapper.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        int padStart = ctx.getResources().getDimensionPixelSize(R.dimen.lb_browse_padding_start);
        int padEnd = ctx.getResources().getDimensionPixelSize(R.dimen.lb_browse_padding_end);
        wrapper.setPaddingRelative(padStart, Math.round(2 * density), padEnd, Math.round(10 * density));
        wrapper.setClipChildren(false);
        wrapper.setClipToPadding(false);

        M3ChipBarView chipBar = new M3ChipBarView(ctx);
        chipBar.setId(R.id.m3_shelf_chip_bar);
        wrapper.addView(chipBar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        return new ViewHolder(wrapper);
    }

    @Override
    protected void onBindRowViewHolder(ViewHolder vh, Object item) {
        super.onBindRowViewHolder(vh, item);
        if (!(item instanceof ShelfChipRow)) {
            return;
        }
        ShelfChipRow row = (ShelfChipRow) item;
        M3ChipBarView chipBar = vh.view.findViewById(R.id.m3_shelf_chip_bar);
        if (chipBar == null) {
            return;
        }
        chipBar.refreshThemeColors();
        chipBar.setChips(row.getChips(), row.getSelectedIndex());
        chipBar.setOnChipSelectedListener((index, chipItem) -> {
            row.setSelectedIndex(index);
            if (row.getListener() != null) {
                row.getListener().onShelfChipClicked(chipItem.id);
            }
        });
    }
}
