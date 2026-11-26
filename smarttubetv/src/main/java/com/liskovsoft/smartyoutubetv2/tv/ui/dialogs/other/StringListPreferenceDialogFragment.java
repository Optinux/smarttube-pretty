package com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.CompoundButtonCompat;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.preference.LeanbackListPreferenceDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.Set;

public class StringListPreferenceDialogFragment extends LeanbackListPreferenceDialogFragment {

    public static StringListPreferenceDialogFragment newInstanceStringList(String key) {
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);

        final StringListPreferenceDialogFragment
                fragment = new StringListPreferenceDialogFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Adapter<ViewHolder> onCreateAdapter() {
        return new AdapterMultiStrings(mEntries, mEntryValues, mInitialSelections);
    }

    public class AdapterMultiStrings extends AdapterMulti {
        public AdapterMultiStrings(CharSequence[] entries, CharSequence[] entryValues, Set<String> initialSelections) {
            super(entries, entryValues, initialSelections);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View view = inflater.inflate(R.layout.dialog_list_preference_item_multi, parent,
                    false);
            ViewHolder holder = new ViewHolder(view, this);
            if (holder.getWidgetView() != null) {
                int checkedColor = ViewUtil.getThemeColor(context, R.attr.m3Primary, R.color.m3_grey_primary);
                int uncheckedColor = ViewUtil.getThemeColor(context, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
                ColorStateList tint = new ColorStateList(
                        new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                        new int[]{checkedColor, uncheckedColor}
                );
                CompoundButtonCompat.setButtonTintList(holder.getWidgetView(), tint);
            }
            if (holder.getTitleView() != null) {
                holder.getTitleView().setTextColor(
                        ViewUtil.getThemeColor(context, R.attr.m3OnSurface, R.color.m3_on_surface));
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.getWidgetView().setChecked(
                    mSelections.contains(mEntryValues[position].toString()));
            holder.getTitleView().setText(mEntries[position]);
        }
    }
}
