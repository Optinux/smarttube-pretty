package com.liskovsoft.smartyoutubetv2.tv.ui.dialogs;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.CompoundButtonCompat;
import androidx.leanback.preference.LeanbackPreferenceFragment;
import androidx.leanback.preference.LeanbackSettingsFragment;
import androidx.leanback.widget.VerticalGridView;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceScreen;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.ChatPreference;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.ChatPreferenceDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.CommentsPreference;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.CommentsPreferenceDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.RadioListPreferenceDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.StringListPreference;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.StringListPreferenceDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.preference.LeanbackListPreferenceDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.List;

@SuppressWarnings("deprecation")
public class AppDialogFragment extends LeanbackSettingsFragment implements AppDialogView {
    private static final String TAG = AppDialogFragment.class.getSimpleName();
    private AppDialogPresenter mPresenter;
    private AppPreferenceManager mManager;
    private boolean mIsTransparent;
    private boolean mIsOverlay;
    private boolean mIsPaused;
    private int mId;

    private static final String PREFERENCE_FRAGMENT_TAG =
            "androidx.leanback.preference.LeanbackSettingsFragment.PREFERENCE_FRAGMENT";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPresenter = AppDialogPresenter.instance(getActivity());
        mPresenter.setView(this);
        mManager = new AppPreferenceManager(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        // Workaround for dialog that are destroyed with the delay (e.g. transparent dialogs)
        mIsPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // Workaround for dialog that are destroyed with the delay (e.g. transparent dialogs)
        mIsPaused = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mPresenter.getView() == this) {
            mPresenter.onViewDestroyed();
        }
    }

    @Override
    public void onPreferenceStartInitialScreen() {
        // FIX: Can not perform this action after onSaveInstanceState
        // Possible fix: Unable to add window -- token android.os.BinderProxy is not valid; is your activity running?
        if (!Utils.checkActivity(getActivity())) {
            return;
        }

        try {
            // Fix mPresenter in null after init stage.
            // Seems concurrency between dialogs.
            mPresenter.setView(this);

            mPresenter.onViewInitialized();
        } catch (IllegalStateException e) {
            // NOP
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment, Preference preference) {
        // Contains only child fragments.
        return false;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment preferenceFragment, PreferenceScreen preferenceScreen) {
        // Contains only child fragments.
        return false;
    }

    private AppPreferenceFragment buildPreferenceFragment(List<OptionCategory> categories, CharSequence title) {
        AppPreferenceFragment fragment = new AppPreferenceFragment();
        fragment.setCategories(categories);
        fragment.setTitle(title);
        fragment.enableTransparent(mIsTransparent);
        return fragment;
    }

    @Override
    public void show(List<OptionCategory> categories, CharSequence title, boolean isExpandable, boolean isTransparent, boolean isOverlay, int id) {
        if (!Utils.checkActivity(getActivity())) {
            return;
        }

        // Only root fragment could make other fragments in the stack transparent
        boolean stackIsEmpty = getChildFragmentManager() != null && getChildFragmentManager().getBackStackEntryCount() == 0;
        mIsTransparent = stackIsEmpty ? isTransparent : mIsTransparent;
        mIsOverlay = isOverlay;
        mId = id;

        if (isExpandable && categories != null && categories.size() == 1) {
            OptionCategory category = categories.get(0);
            if (category.options != null) {
                onPreferenceDisplayDialog(null, mManager.createPreference(category));
            }
        } else {
            AppPreferenceFragment fragment = buildPreferenceFragment(categories, title);
            startPreferenceFragment(fragment);
        }
    }

    @Override
    public boolean onPreferenceDisplayDialog(@Nullable PreferenceFragment caller, @NonNull Preference pref) {
        // Fix: IllegalStateException: Activity has been destroyed
        // Possible fix: Unable to add window -- token android.os.BinderProxy is not valid; is your activity running?
        if (!Utils.checkActivity(getActivity())) {
            return false;
        }

        if (pref instanceof StringListPreference) {
            StringListPreference listPreference = (StringListPreference) pref;
            StringListPreferenceDialogFragment f = StringListPreferenceDialogFragment.newInstanceStringList(listPreference.getKey());
            f.enableTransparent(mIsTransparent);
            f.setTargetFragment(caller, 0);
            f.setPreference(pref);
            startPreferenceFragment(f);

            return true;
        } else if (pref instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) pref;
            RadioListPreferenceDialogFragment f = RadioListPreferenceDialogFragment.newInstanceSingle(listPreference.getKey());
            f.enableTransparent(mIsTransparent);
            f.setTargetFragment(caller, 0);
            f.setPreference(pref);
            startPreferenceFragment(f);

            return true;
        } else if (pref instanceof ChatPreference) {
            ChatPreference chatPreference = (ChatPreference) pref;
            ChatPreferenceDialogFragment f = ChatPreferenceDialogFragment.newInstance(chatPreference.getChatReceiver(), chatPreference.getKey());
            f.enableTransparent(mIsTransparent);
            f.setTargetFragment(caller, 0);
            f.setPreference(pref);
            startPreferenceFragment(f);

            return true;
        } else if (pref instanceof CommentsPreference) {
            ((MotherActivity) getActivity()).enableThrottleKeyDown(true);
            CommentsPreference commentsPreference = (CommentsPreference) pref;
            CommentsPreferenceDialogFragment f = CommentsPreferenceDialogFragment.newInstance(commentsPreference.getCommentsReceiver(), commentsPreference.getKey());
            f.enableTransparent(mIsTransparent);
            f.setTargetFragment(caller, 0);
            f.setPreference(pref);
            startPreferenceFragment(f);

            return true;
        } else if (pref instanceof MultiSelectListPreference) {
            MultiSelectListPreference listPreference = (MultiSelectListPreference) pref;
            LeanbackListPreferenceDialogFragment f = LeanbackListPreferenceDialogFragment.newInstanceMulti(listPreference.getKey());
            f.enableTransparent(mIsTransparent);
            f.setTargetFragment(caller, 0);
            f.setPreference(pref);
            startPreferenceFragment(f);
        }
        // TODO
        // else if (pref instanceof EditTextPreference) {
        //
        //        }
        else {
            // Single button item. Imitate click on it (expandable = true).
            if (pref.getOnPreferenceClickListener() != null) {
                pref.getOnPreferenceClickListener().onPreferenceClick(pref);
            }

            return false;
        }

        // NOTE: Transparent CheckedList should be placed here (just in case you'll need it).

        //return super.onPreferenceDisplayDialog(caller, pref);
        return true;
    }

    /**
     * Fix possible state loss!!!
     */
    @Override
    public void startPreferenceFragment(@NonNull Fragment fragment) {
        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        final Fragment prevFragment =
                getChildFragmentManager().findFragmentByTag(PREFERENCE_FRAGMENT_TAG);
        if (prevFragment != null) {
            transaction
                    .setCustomAnimations(
                            R.animator.m3_shared_axis_x_enter,
                            R.animator.m3_shared_axis_x_exit,
                            R.animator.m3_shared_axis_x_enter,
                            R.animator.m3_shared_axis_x_exit)
                    .addToBackStack(null)
                    .replace(R.id.settings_preference_fragment_container, fragment,
                            PREFERENCE_FRAGMENT_TAG);
        } else {
            transaction
                    .setCustomAnimations(
                            R.animator.m3_side_sheet_enter,
                            R.animator.m3_side_sheet_exit)
                    .add(R.id.settings_preference_fragment_container, fragment,
                            PREFERENCE_FRAGMENT_TAG);
        }
        // Fix possible state loss!!!
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void finish() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void goBack() {
        if (canGoBack()) {
            getChildFragmentManager().popBackStack();
        } else {
            finish();
        }
    }

    @Override
    public void clearBackstack() {
        // this manager holds entire back stack
        Helpers.setField(this, "mChildFragmentManager", null);
    }

    @Override
    public boolean canGoBack() {
        return getChildFragmentManager() != null && getChildFragmentManager().getBackStackEntryCount() > 0;
    }

    @Override
    public boolean isShown() {
        return isVisible() && getUserVisibleHint();
    }

    @Override
    public boolean isTransparent() {
        return mIsTransparent;
    }

    @Override
    public boolean isOverlay() {
        return mIsOverlay;
    }

    @Override
    public boolean isPaused() {
        return mIsPaused;
    }

    @Override
    public int getViewId() {
        return mId;
    }

    public void onFinish() {
        mPresenter.onFinish();
    }

    /**
     * Applies Material 3 Expressive Side-Sheet surface colors, rounded container outline,
     * list padding/spacing, and rounded pill focus selectors when not in transparent mode.
     */
    public static void applyM3SideSheetStyling(Context context, @Nullable View parentRootView, @Nullable View fragmentView) {
        if (context == null || fragmentView == null) {
            return;
        }

        int outlineVariant = ViewUtil.getThemeColor(
                context, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        float density = context.getResources().getDisplayMetrics().density;

        if (parentRootView != null) {
            int backdropColor = androidx.core.content.ContextCompat.getColor(context, R.color.m3_dialog_backdrop);
            parentRootView.setBackgroundColor(
                    androidx.core.graphics.ColorUtils.setAlphaComponent(backdropColor, 145));
            View mainContainer = parentRootView.findViewById(R.id.settings_preference_fragment_container);
            if (mainContainer != null) {
                float sheetRadiusPx = context.getResources().getDimension(R.dimen.m3_dialog_corner_radius);
                ViewUtil.applyRoundedOutline(mainContainer, sheetRadiusPx);
                if (android.os.Build.VERSION.SDK_INT >= 23 && mainContainer instanceof android.widget.FrameLayout) {
                    int borderPx = Math.max(1, Math.round(1f * density));
                    mainContainer.setForeground(ViewUtil.createRoundedDrawable(
                            Color.TRANSPARENT,
                            androidx.core.graphics.ColorUtils.setAlphaComponent(outlineVariant, 170),
                            borderPx,
                            sheetRadiusPx));
                }
            }
        }

        View mainFrame = fragmentView.findViewById(R.id.main_frame);
        if (mainFrame != null) {
            mainFrame.setBackgroundColor(ViewUtil.getThemeColor(
                    context, R.attr.m3SurfaceContainer, R.color.m3_grey_surface_container));
        }

        View titleContainer = fragmentView.findViewById(R.id.decor_title_container);
        if (titleContainer != null) {
            titleContainer.setBackgroundColor(ViewUtil.getThemeColor(
                    context, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high));
            if (titleContainer instanceof ViewGroup) {
                ViewGroup titleGroup = (ViewGroup) titleContainer;
                for (int i = 0; i < titleGroup.getChildCount(); i++) {
                    View child = titleGroup.getChildAt(i);
                    if (child.getId() != R.id.decor_title && child.getLayoutParams() != null && child.getLayoutParams().height <= 4) {
                        child.setBackgroundColor(outlineVariant);
                    }
                }
            }
        }

        int primaryColor = ViewUtil.getThemeColor(context, R.attr.m3Primary, R.color.m3_grey_primary);
        int onSurfaceVariant = ViewUtil.getThemeColor(context, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
        ColorStateList buttonTint = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{primaryColor, onSurfaceVariant});

        View listView = fragmentView.findViewById(android.R.id.list);
        if (!(listView instanceof VerticalGridView)) {
            listView = fragmentView.findViewById(R.id.list);
        }
        if (listView instanceof VerticalGridView) {
            VerticalGridView gridView = (VerticalGridView) listView;
            int padH = Math.round(14 * density);
            int padTop = Math.round(10 * density);
            int padBottom = Math.round(16 * density);
            gridView.setPaddingRelative(padH, padTop, padH, padBottom);
            gridView.setClipToPadding(true);
            gridView.setClipChildren(true);
            gridView.setVerticalSpacing(Math.round(3 * density));

            float outerItemRadius = context.getResources().getDimension(R.dimen.m3_card_small_corner_radius);
            float innerItemRadius = context.getResources().getDimension(R.dimen.m3_segment_inner_corner_radius);
            int strokeWidthPx = context.getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);
            int surfaceContainerHigh = ViewUtil.getThemeColor(
                    context, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
            int restingFill = androidx.core.graphics.ColorUtils.setAlphaComponent(surfaceContainerHigh, 190);
            int focusedFill = ViewUtil.getThemeColor(
                    context, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container);
            int focusedStroke = ViewUtil.getThemeColor(
                    context, R.attr.m3Outline, R.color.m3_grey_outline);

            gridView.setOnChildLaidOutListener((parent, child, position, id) -> {
                if (child != null) {
                    androidx.recyclerview.widget.RecyclerView.Adapter<?> adapter = gridView.getAdapter();
                    int count = adapter != null ? adapter.getItemCount() : 0;
                    boolean isFirst = position == 0;
                    boolean isLast = count > 0 && position == count - 1;
                    float[] radii = M3Motion.buildSegmentedRadii(outerItemRadius, innerItemRadius, isFirst, isLast);

                    boolean initiallyFocused = child.hasFocus();
                    M3Motion.cancelFocusAnimations(child);
                    M3Motion.applyCardFocusTransition(
                            child,
                            initiallyFocused,
                            initiallyFocused ? restingFill : focusedFill,
                            initiallyFocused ? focusedFill : restingFill,
                            initiallyFocused ? Color.TRANSPARENT : focusedStroke,
                            initiallyFocused ? focusedStroke : Color.TRANSPARENT,
                            initiallyFocused ? 0 : strokeWidthPx,
                            initiallyFocused ? strokeWidthPx : 0,
                            outerItemRadius,
                            radii,
                            null);
                    if (child.getTag(R.id.m3_focus_listener_attached) == null) {
                        // Store original listener once so recycled rows do not wrap listeners repeatedly
                        final View.OnFocusChangeListener prevListener = child.getOnFocusChangeListener();
                        child.setTag(R.id.m3_focus_listener_attached, Boolean.TRUE);
                        child.setOnFocusChangeListener((v, hasFocus) -> {
                            if (prevListener != null) {
                                prevListener.onFocusChange(v, hasFocus);
                            }
                            M3Motion.applyCardFocusTransition(
                                    v,
                                    hasFocus,
                                    hasFocus ? restingFill : focusedFill,
                                    hasFocus ? focusedFill : restingFill,
                                    hasFocus ? Color.TRANSPARENT : focusedStroke,
                                    hasFocus ? focusedStroke : Color.TRANSPARENT,
                                    hasFocus ? 0 : strokeWidthPx,
                                    hasFocus ? strokeWidthPx : 0,
                                    outerItemRadius,
                                    null,
                                    null);
                            M3Motion.animateFocusScale(v, hasFocus, 1.012f);
                        });
                    }
                    tintCompoundButtons(child, buttonTint);
                }
            });
        }
    }

    public static void tintCompoundButtons(View view, ColorStateList tint) {
        if (view instanceof CompoundButton) {
            CompoundButtonCompat.setButtonTintList((CompoundButton) view, tint);
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                tintCompoundButtons(vg.getChildAt(i), tint);
            }
        }
    }
    
    public static class AppPreferenceFragment extends LeanbackPreferenceFragment {
        private static final String TAG = AppPreferenceFragment.class.getSimpleName();
        private List<OptionCategory> mCategories;
        private Context mExtractedContext;
        private AppPreferenceManager mManager;
        private CharSequence mTitle;
        private boolean mIsTransparent;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            // Note, place in field with different name to avoid field overlapping
            mExtractedContext = (Context) Helpers.getField(this, "mStyledContext");
            // Note, don't use external manager (probable focus lost and visual bugs)
            mManager = new AppPreferenceManager(mExtractedContext);

            initPrefs();

            Log.d(TAG, "onCreatePreferences");
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);

            if (view != null) {
                View parentView = getParentFragment() != null ? getParentFragment().getView() : null;
                if (mIsTransparent) {
                    // Enable transparent shadow outline on parent (R.id.settings_preference_fragment_container)
                    ViewUtil.enableTransparentDialog(getActivity(), parentView);
                    // Enable transparency on child fragment itself (isn't attached to parent yet)
                    ViewUtil.enableTransparentDialog(getActivity(), view);
                } else {
                    applyM3SideSheetStyling(getActivity(), parentView, view);
                }
            }

            return view;
        }

        private void initPrefs() {
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(mExtractedContext);
            setPreferenceScreen(screen);

            screen.setTitle(mTitle);

            addPreferences(screen);
        }

        private void addPreferences(PreferenceScreen screen) {
            if (mCategories != null) {
                for (OptionCategory category : mCategories) {
                    if (category.options != null) {
                        screen.addPreference(mManager.createPreference(category));
                    }
                }
            }
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            super.onDisplayPreferenceDialog(preference);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            return super.onPreferenceTreeClick(preference);
        }

        public void setCategories(List<OptionCategory> categories) {
            mCategories = categories;
        }

        public void setTitle(CharSequence title) {
            super.setTitle(title);

            mTitle = title;
        }

        public void enableTransparent(boolean enable) {
            mIsTransparent = enable;
        }
    }
}