package com.liskovsoft.smartyoutubetv2.tv.ui.browse.settings;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.clickable.LinkifyCompat;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e.M3QuickToggleRow;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders {@link OptionCategory} lists produced by SmartTube settings presenters
 * directly inside the Settings Hub right-hand detail pane using M3 Expressive controls.
 */
public final class InlineOptionsRenderer {
    public interface Host {
        void attachReturnToRailOnLeft(@NonNull View view);
        void onOptionsMutated();
    }

    private InlineOptionsRenderer() {
    }

    public static void renderCategoryHeader(
            @NonNull Context ctx,
            @NonNull LinearLayout container,
            @NonNull SettingsItem item,
            @Nullable CharSequence overrideTitle,
            @Nullable TextView[] outSubtitleHolder) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int primaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        int onPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);
        int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
        int onSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
        int surfaceHigh = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int outlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        float heroRadius = ctx.getResources().getDimension(R.dimen.m3_card_large_corner_radius);
        int hairlinePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        LinearLayout headerRow = new LinearLayout(ctx);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        int pad = Math.round(16 * density);
        headerRow.setPadding(pad, pad, pad, pad);
        headerRow.setBackground(ViewUtil.createRoundedDrawable(surfaceHigh, outlineVariant, hairlinePx, heroRadius));

        if (item.imageResId > 0) {
            FrameLayout iconBox = new FrameLayout(ctx);
            int boxSize = Math.round(48 * density);
            LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(boxSize, boxSize);
            boxLp.setMarginEnd(Math.round(16 * density));
            iconBox.setBackground(ViewUtil.createRoundedDrawable(primaryContainer, 0, 0, Math.round(14 * density)));

            ImageView icon = new ImageView(ctx);
            int iconSize = Math.round(24 * density);
            FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER);
            icon.setImageDrawable(ContextCompat.getDrawable(ctx, item.imageResId));
            icon.setColorFilter(onPrimaryContainer, PorterDuff.Mode.SRC_IN);
            iconBox.addView(icon, iconLp);
            headerRow.addView(iconBox, boxLp);
        }

        LinearLayout titleCol = new LinearLayout(ctx);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(ctx);
        titleView.setText(!TextUtils.isEmpty(overrideTitle) ? overrideTitle : item.title);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        titleView.setTextColor(onSurface);
        titleCol.addView(titleView);

        String summary = SettingsHubHelper.getSubtitleSummary(ctx, item);
        TextView summaryView = new TextView(ctx);
        summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        summaryView.setTextColor(onSurfaceVariant);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = Math.round(3 * density);
        if (!TextUtils.isEmpty(summary)) {
            summaryView.setText(summary);
        } else {
            summaryView.setVisibility(View.GONE);
        }
        titleCol.addView(summaryView, sLp);
        if (outSubtitleHolder != null && outSubtitleHolder.length > 0) {
            outSubtitleHolder[0] = summaryView;
        }

        headerRow.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int sideMargin = Math.round(3 * density);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.setMarginStart(sideMargin);
        headerLp.setMarginEnd(sideMargin);
        headerLp.bottomMargin = Math.round(14 * density);
        container.addView(headerRow, headerLp);
    }

    public static void renderCategories(
            @NonNull Context ctx,
            @NonNull LinearLayout container,
            @NonNull List<OptionCategory> categories,
            @NonNull Host host) {
        if (categories.isEmpty()) {
            addInfoMessage(ctx, container, ctx.getString(R.string.settings_empty_options));
            return;
        }

        float density = ctx.getResources().getDisplayMetrics().density;
        int segmentGap = Math.round(4 * density);
        int sectionGap = Math.round(14 * density);

        int i = 0;
        while (i < categories.size()) {
            OptionCategory cat = categories.get(i);
            if (isStandaloneRowCategory(cat)) {
                int start = i;
                while (i < categories.size() && isStandaloneRowCategory(categories.get(i))) {
                    i++;
                }
                int count = i - start;
                for (int k = 0; k < count; k++) {
                    OptionCategory c = categories.get(start + k);
                    boolean isLastInGroup = (k == count - 1);
                    M3QuickToggleRow row = buildStandaloneRow(ctx, c, host);
                    if (row != null) {
                        row.setSegmentedPosition(k == 0, isLastInGroup);
                        addSpacedRow(ctx, container, row, isLastInGroup ? sectionGap : segmentGap);
                    }
                }
            } else {
                renderComplexCategory(ctx, container, cat, categories.size(), host, sectionGap, segmentGap);
                i++;
            }
        }
    }

    private static boolean isStandaloneRowCategory(@Nullable OptionCategory category) {
        if (category == null || category.options == null || category.options.isEmpty()) {
            return false;
        }
        return (category.type == OptionCategory.TYPE_SINGLE_SWITCH
                || category.type == OptionCategory.TYPE_SINGLE_BUTTON)
                && category.options.size() == 1;
    }

    @Nullable
    private static M3QuickToggleRow buildStandaloneRow(
            @NonNull Context ctx,
            @NonNull OptionCategory category,
            @NonNull Host host) {
        if (category.options == null || category.options.isEmpty()) {
            return null;
        }
        OptionItem opt = category.options.get(0);
        M3QuickToggleRow row = new M3QuickToggleRow(ctx);
        CharSequence title = !TextUtils.isEmpty(opt.getTitle()) ? opt.getTitle() : category.title;
        row.setTitle(title != null ? title : "");
        row.setSubtitle(opt.getDescription());
        host.attachReturnToRailOnLeft(row);

        if (category.type == OptionCategory.TYPE_SINGLE_SWITCH) {
            final boolean[] state = {opt.isSelected()};
            row.bindSwitch(state[0], false);
            row.setOnClickListener(v -> {
                state[0] = !state[0];
                row.bindSwitch(state[0], true);
                opt.onSelect(state[0]);
                host.onOptionsMutated();
            });
        } else {
            row.bindTrailingIcon(R.drawable.ic_m3_chevron_right, false);
            row.setOnClickListener(v -> {
                opt.onSelect(true);
                host.onOptionsMutated();
            });
        }
        return row;
    }

    private static void renderComplexCategory(
            @NonNull Context ctx,
            @NonNull LinearLayout container,
            @NonNull OptionCategory category,
            int totalCategories,
            @NonNull Host host,
            int sectionGap,
            int segmentGap) {
        if (category.type == OptionCategory.TYPE_LONG_TEXT) {
            renderLongTextCategory(ctx, container, category, host, sectionGap);
            return;
        }

        List<OptionItem> options = category.options != null ? category.options : java.util.Collections.emptyList();
        if (options.isEmpty()) {
            if (!TextUtils.isEmpty(category.title)) {
                addSectionTitle(ctx, container, category.title);
            }
            addInfoMessage(ctx, container, ctx.getString(R.string.settings_empty_options));
            return;
        }

        if (category.type == OptionCategory.TYPE_RADIO_LIST) {
            boolean directExpanded = totalCategories == 1 || (totalCategories <= 3 && options.size() <= 8);
            renderRadioCategory(ctx, container, category, options, directExpanded, host, sectionGap, segmentGap);
        } else if (category.type == OptionCategory.TYPE_CHECKBOX_LIST) {
            boolean directExpanded = totalCategories == 1 || (totalCategories <= 2 && options.size() <= 8);
            renderCheckboxCategory(ctx, container, category, options, directExpanded, host, sectionGap, segmentGap);
        } else {
            // TYPE_STRING_LIST or fallback list of actionable items
            boolean directExpanded = totalCategories == 1 || (totalCategories <= 3 && options.size() <= 6);
            renderActionListCategory(ctx, container, category, options, directExpanded, host, sectionGap, segmentGap);
        }
    }

    private static void renderRadioCategory(
            @NonNull Context ctx,
            @NonNull LinearLayout container,
            @NonNull OptionCategory category,
            @NonNull List<OptionItem> options,
            boolean startExpanded,
            @NonNull Host host,
            int sectionGap,
            int segmentGap) {
        int initialIndex = -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).isSelected()) {
                initialIndex = i;
                break;
            }
        }
        final int[] selectedIndex = {initialIndex};
        final List<M3QuickToggleRow> optionRows = new ArrayList<>(options.size());

        // Single-category screens render rows directly for fast 1-click access
        boolean showAccordionHeader = !startExpanded || !TextUtils.isEmpty(category.title);
        M3QuickToggleRow headerRow = null;
        if (showAccordionHeader) {
            if (startExpanded) {
                addSectionTitle(ctx, container, category.title);
            } else {
                headerRow = new M3QuickToggleRow(ctx);
                headerRow.setTitle(category.title != null ? category.title : "");
                headerRow.bindValuePill(resolveSelectedTitle(ctx, options, selectedIndex[0]));
                headerRow.setSegmentedPosition(true, true);
                host.attachReturnToRailOnLeft(headerRow);
                addSpacedRow(ctx, container, headerRow, sectionGap);
            }
        }

        LinearLayout listContainer = new LinearLayout(ctx);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setClipChildren(false);
        listContainer.setClipToPadding(false);
        listContainer.setVisibility(startExpanded ? View.VISIBLE : View.GONE);

        final M3QuickToggleRow accordionHeader = headerRow;
        for (int i = 0; i < options.size(); i++) {
            final int idx = i;
            OptionItem opt = options.get(i);
            M3QuickToggleRow row = new M3QuickToggleRow(ctx);
            row.setTitle(opt.getTitle() != null ? opt.getTitle() : "");
            row.setSubtitle(opt.getDescription());
            row.bindTrailingIcon(idx == selectedIndex[0] ? R.drawable.ic_m3_check : 0, true);
            boolean isFirst = accordionHeader == null && i == 0;
            boolean isLast = i == options.size() - 1;
            row.setSegmentedPosition(isFirst, isLast);
            host.attachReturnToRailOnLeft(row);

            row.setOnClickListener(v -> {
                selectedIndex[0] = idx;
                for (int j = 0; j < optionRows.size(); j++) {
                    optionRows.get(j).bindTrailingIcon(j == idx ? R.drawable.ic_m3_check : 0, true);
                }
                if (accordionHeader != null) {
                    accordionHeader.bindValuePill(resolveSelectedTitle(ctx, options, idx));
                }
                opt.onSelect(true);
                host.onOptionsMutated();
            });

            optionRows.add(row);
            addSpacedRow(ctx, listContainer, row, isLast ? sectionGap : segmentGap);
        }

        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        container.addView(listContainer, listLp);

        if (accordionHeader != null) {
            final boolean[] expanded = {false};
            accordionHeader.setOnClickListener(v -> {
                expanded[0] = !expanded[0];
                accordionHeader.setSegmentedPosition(true, !expanded[0]);
                updateRowBottomMargin(accordionHeader, expanded[0] ? segmentGap : sectionGap);
                listContainer.setVisibility(expanded[0] ? View.VISIBLE : View.GONE);
                if (expanded[0]) {
                    M3Motion.animateEnter(listContainer, 8f * ctx.getResources().getDisplayMetrics().density, 0f);
                }
            });
        }
    }

    private static void renderCheckboxCategory(
            @NonNull Context ctx,
            @NonNull LinearLayout container,
            @NonNull OptionCategory category,
            @NonNull List<OptionItem> options,
            boolean startExpanded,
            @NonNull Host host,
            int sectionGap,
            int segmentGap) {
        final boolean[] checkedStates = new boolean[options.size()];
        for (int i = 0; i < options.size(); i++) {
            checkedStates[i] = options.get(i).isSelected();
        }
        final List<M3QuickToggleRow> optionRows = new ArrayList<>(options.size());

        M3QuickToggleRow headerRow = null;
        if (startExpanded) {
            if (!TextUtils.isEmpty(category.title)) {
                addSectionTitle(ctx, container, category.title);
            }
        } else {
            headerRow = new M3QuickToggleRow(ctx);
            headerRow.setTitle(category.title != null ? category.title : "");
            headerRow.bindValuePill(formatCheckedCount(ctx, checkedStates));
            headerRow.setSegmentedPosition(true, true);
            host.attachReturnToRailOnLeft(headerRow);
            addSpacedRow(ctx, container, headerRow, sectionGap);
        }

        LinearLayout listContainer = new LinearLayout(ctx);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setClipChildren(false);
        listContainer.setClipToPadding(false);
        listContainer.setVisibility(startExpanded ? View.VISIBLE : View.GONE);

        final M3QuickToggleRow accordionHeader = headerRow;
        for (int i = 0; i < options.size(); i++) {
            final int idx = i;
            OptionItem opt = options.get(i);
            M3QuickToggleRow row = new M3QuickToggleRow(ctx);
            row.setTitle(opt.getTitle() != null ? opt.getTitle() : "");
            row.setSubtitle(opt.getDescription());
            row.bindSwitch(checkedStates[i], false);
            boolean isFirst = accordionHeader == null && i == 0;
            boolean isLast = i == options.size() - 1;
            row.setSegmentedPosition(isFirst, isLast);
            host.attachReturnToRailOnLeft(row);

            row.setOnClickListener(v -> {
                boolean next = !checkedStates[idx];
                if (next && opt.getRequired() != null) {
                    for (OptionItem req : opt.getRequired()) {
                        if (req != null && !isOptionChecked(options, checkedStates, req)) {
                            Toast.makeText(
                                    ctx,
                                    ctx.getString(R.string.require_checked, req.getTitle()),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }
                if (next && opt.getRadio() != null) {
                    for (OptionItem radioSibling : opt.getRadio()) {
                        if (radioSibling != null && radioSibling != opt) {
                            int sibIdx = options.indexOf(radioSibling);
                            if (sibIdx >= 0 && sibIdx != idx && checkedStates[sibIdx]) {
                                checkedStates[sibIdx] = false;
                                optionRows.get(sibIdx).bindSwitch(false, true);
                                radioSibling.onSelect(false);
                            }
                        }
                    }
                }
                checkedStates[idx] = next;
                row.bindSwitch(next, true);
                opt.onSelect(next);
                if (accordionHeader != null) {
                    accordionHeader.bindValuePill(formatCheckedCount(ctx, checkedStates));
                }
                host.onOptionsMutated();
            });

            optionRows.add(row);
            addSpacedRow(ctx, listContainer, row, isLast ? sectionGap : segmentGap);
        }

        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        container.addView(listContainer, listLp);

        if (accordionHeader != null) {
            final boolean[] expanded = {false};
            accordionHeader.setOnClickListener(v -> {
                expanded[0] = !expanded[0];
                accordionHeader.setSegmentedPosition(true, !expanded[0]);
                updateRowBottomMargin(accordionHeader, expanded[0] ? segmentGap : sectionGap);
                listContainer.setVisibility(expanded[0] ? View.VISIBLE : View.GONE);
                if (expanded[0]) {
                    M3Motion.animateEnter(listContainer, 8f * ctx.getResources().getDisplayMetrics().density, 0f);
                }
            });
        }
    }

    private static void renderActionListCategory(
            @NonNull Context ctx,
            @NonNull LinearLayout container,
            @NonNull OptionCategory category,
            @NonNull List<OptionItem> options,
            boolean startExpanded,
            @NonNull Host host,
            int sectionGap,
            int segmentGap) {
        M3QuickToggleRow headerRow = null;
        if (startExpanded) {
            if (!TextUtils.isEmpty(category.title)) {
                addSectionTitle(ctx, container, category.title);
            }
        } else {
            headerRow = new M3QuickToggleRow(ctx);
            headerRow.setTitle(category.title != null ? category.title : "");
            headerRow.bindValuePill(String.valueOf(options.size()));
            headerRow.setSegmentedPosition(true, true);
            host.attachReturnToRailOnLeft(headerRow);
            addSpacedRow(ctx, container, headerRow, sectionGap);
        }

        LinearLayout listContainer = new LinearLayout(ctx);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setClipChildren(false);
        listContainer.setClipToPadding(false);
        listContainer.setVisibility(startExpanded ? View.VISIBLE : View.GONE);

        for (int i = 0; i < options.size(); i++) {
            OptionItem opt = options.get(i);
            M3QuickToggleRow row = new M3QuickToggleRow(ctx);
            row.setTitle(opt.getTitle() != null ? opt.getTitle() : "");
            row.setSubtitle(opt.getDescription());
            row.bindTrailingIcon(R.drawable.ic_m3_chevron_right, false);
            boolean isFirst = headerRow == null && i == 0;
            boolean isLast = i == options.size() - 1;
            row.setSegmentedPosition(isFirst, isLast);
            host.attachReturnToRailOnLeft(row);
            row.setOnClickListener(v -> {
                opt.onSelect(true);
                host.onOptionsMutated();
            });
            addSpacedRow(ctx, listContainer, row, isLast ? sectionGap : segmentGap);
        }

        container.addView(listContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (headerRow != null) {
            final M3QuickToggleRow accordionHeader = headerRow;
            final boolean[] expanded = {false};
            accordionHeader.setOnClickListener(v -> {
                expanded[0] = !expanded[0];
                accordionHeader.setSegmentedPosition(true, !expanded[0]);
                updateRowBottomMargin(accordionHeader, expanded[0] ? segmentGap : sectionGap);
                listContainer.setVisibility(expanded[0] ? View.VISIBLE : View.GONE);
                if (expanded[0]) {
                    M3Motion.animateEnter(listContainer, 8f * ctx.getResources().getDisplayMetrics().density, 0f);
                }
            });
        }
    }

    private static void renderLongTextCategory(
            @NonNull Context ctx,
            @NonNull LinearLayout container,
            @NonNull OptionCategory category,
            @NonNull Host host,
            int sectionGap) {
        if (!TextUtils.isEmpty(category.title)) {
            addSectionTitle(ctx, container, category.title);
        }
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (category.options != null) {
            for (OptionItem opt : category.options) {
                if (!TextUtils.isEmpty(opt.getTitle())) {
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append(opt.getTitle());
                }
            }
        }

        float density = ctx.getResources().getDisplayMetrics().density;
        int surfaceHigh = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int outlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
        int primary = ViewUtil.getThemeColor(ctx, R.attr.m3Primary, R.color.m3_grey_primary);
        float cardRadius = ctx.getResources().getDimension(R.dimen.m3_card_corner_radius);
        int hairlinePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        TextView textView = new TextView(ctx);
        int pad = Math.round(16 * density);
        textView.setPadding(pad, pad, pad, pad);
        textView.setText(sb);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setLineSpacing(0f, 1.3f);
        textView.setTextColor(onSurface);
        textView.setLinkTextColor(primary);
        textView.setFocusable(true);
        textView.setFocusableInTouchMode(true);
        textView.setBackground(ViewUtil.createRoundedDrawable(surfaceHigh, outlineVariant, hairlinePx, cardRadius));
        LinkifyCompat.addLinks(textView, LinkifyCompat.WEB_URLS | LinkifyCompat.EMAIL_ADDRESSES);
        host.attachReturnToRailOnLeft(textView);
        addSpacedRow(ctx, container, textView, sectionGap);
    }

    private static void addSectionTitle(@NonNull Context ctx, @NonNull LinearLayout container, @NonNull CharSequence title) {
        float density = ctx.getResources().getDisplayMetrics().density;
        TextView label = new TextView(ctx);
        label.setText(title);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        label.setTextColor(ViewUtil.getThemeColor(ctx, R.attr.m3Primary, R.color.m3_grey_primary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(Math.round(6 * density));
        lp.topMargin = Math.round(2 * density);
        lp.bottomMargin = Math.round(8 * density);
        container.addView(label, lp);
    }

    public static void addInfoMessage(@NonNull Context ctx, @NonNull LinearLayout container, @NonNull CharSequence msg) {
        float density = ctx.getResources().getDisplayMetrics().density;
        TextView tv = new TextView(ctx);
        tv.setText(msg);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTextColor(ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(Math.round(6 * density));
        lp.bottomMargin = Math.round(12 * density);
        container.addView(tv, lp);
    }

    private static void addSpacedRow(
            @NonNull Context ctx,
            @NonNull LinearLayout container,
            @NonNull View view,
            int bottomMarginPx) {
        int sideMarginPx = Math.round(3 * ctx.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(sideMarginPx);
        lp.setMarginEnd(sideMarginPx);
        lp.bottomMargin = bottomMarginPx;
        container.addView(view, lp);
    }

    private static void updateRowBottomMargin(@NonNull View view, int bottomMarginPx) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) lp).bottomMargin = bottomMarginPx;
            view.setLayoutParams(lp);
        }
    }

    @NonNull
    private static CharSequence resolveSelectedTitle(
            @NonNull Context ctx,
            @NonNull List<OptionItem> options,
            int selectedIndex) {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            CharSequence title = options.get(selectedIndex).getTitle();
            if (!TextUtils.isEmpty(title)) {
                return title;
            }
        }
        return ctx.getString(R.string.settings_none_selected);
    }

    @NonNull
    private static String formatCheckedCount(@NonNull Context ctx, @NonNull boolean[] states) {
        int enabled = 0;
        for (boolean s : states) {
            if (s) enabled++;
        }
        return ctx.getString(R.string.settings_selected_count, enabled, states.length);
    }

    private static boolean isOptionChecked(
            @NonNull List<OptionItem> options,
            @NonNull boolean[] states,
            @NonNull OptionItem target) {
        int idx = options.indexOf(target);
        if (idx >= 0 && idx < states.length) {
            return states[idx];
        }
        return target.isSelected();
    }
}
