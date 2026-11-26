package com.liskovsoft.smartyoutubetv2.tv.presenter;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Row;

import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e.M3ChipBarView.ChipItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Leanback Row model backing the quick-jump shelf filter chip strip below the Hero Spotlight.
 */
public class ShelfChipRow extends Row {
    public interface OnShelfChipClickedListener {
        void onShelfChipClicked(int rowPosition);
    }

    private final List<ChipItem> mChips = new ArrayList<>();
    private final OnShelfChipClickedListener mListener;
    private int mSelectedIndex;

    public ShelfChipRow(@NonNull OnShelfChipClickedListener listener) {
        mListener = listener;
    }

    public void setChips(@NonNull List<ChipItem> chips, int selectedIndex) {
        mChips.clear();
        mChips.addAll(chips);
        mSelectedIndex = Math.max(0, Math.min(selectedIndex, Math.max(0, mChips.size() - 1)));
    }

    public List<ChipItem> getChips() {
        return Collections.unmodifiableList(mChips);
    }

    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        mSelectedIndex = selectedIndex;
    }

    public OnShelfChipClickedListener getListener() {
        return mListener;
    }
}
