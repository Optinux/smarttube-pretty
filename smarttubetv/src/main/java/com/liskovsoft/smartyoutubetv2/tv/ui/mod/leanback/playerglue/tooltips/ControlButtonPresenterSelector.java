/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tooltips;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import androidx.leanback.R;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.PaddingAction;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * Displays primary and secondary controls for a {@link PlaybackControlsRow}.
 *
 * Binds to items of type {@link Action}.
 */
public class ControlButtonPresenterSelector extends PresenterSelector {
    private final ControlButtonPresenter mPrimaryPresenter =
            new ControlButtonPresenter(R.layout.lb_control_button_primary);
    private final ControlButtonPresenter mSecondaryPresenter =
            new ControlButtonPresenter(R.layout.lb_control_button_secondary);
    private final Presenter[] mPresenters = new Presenter[]{mPrimaryPresenter};

    public ControlButtonPresenterSelector(boolean tooltipsEnabled) {
        mPrimaryPresenter.tooltipsEnabled = mSecondaryPresenter.tooltipsEnabled = tooltipsEnabled;
    }

    /**
     * Returns the presenter for primary controls.
     */
    public Presenter getPrimaryPresenter() {
        return mPrimaryPresenter;
    }

    /**
     * Returns the presenter for secondary controls.
     */
    public Presenter getSecondaryPresenter() {
        return mSecondaryPresenter;
    }

    /**
     * Always returns the presenter for primary controls.
     */
    @Override
    public Presenter getPresenter(Object item) {
        return mPrimaryPresenter;
    }

    @Override
    public Presenter[] getPresenters() {
        return mPresenters;
    }

    static class ActionViewHolder extends Presenter.ViewHolder {
        ImageView mIcon;
        TextView mLabel;
        View mFocusableView;

        public ActionViewHolder(View view) {
            super(view);
            mIcon = (ImageView) view.findViewById(R.id.icon);
            mLabel = (TextView) view.findViewById(R.id.label);
            mFocusableView = view.findViewById(R.id.button);
        }
    }

    public static class ControlButtonPresenter extends Presenter {
        private int mLayoutResourceId;
        private boolean tooltipsEnabled;

        ControlButtonPresenter(int layoutResourceId) {
            mLayoutResourceId = layoutResourceId;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            Context context = parent.getContext();
            View v = LayoutInflater.from(context)
                    .inflate(mLayoutResourceId, parent, false);
            ActionViewHolder vh = new ActionViewHolder(v);
            initButtonAppearance(context, vh);
            return vh;
        }

        private void initButtonAppearance(Context context, ActionViewHolder vh) {
            if (vh.mFocusableView == null) {
                return;
            }
            if (vh.mFocusableView instanceof ImageView) {
                ((ImageView) vh.mFocusableView).setImageDrawable(null);
            }
            boolean isPrimary = mLayoutResourceId == R.layout.lb_control_button_primary;
            int surfaceColor = ViewUtil.getThemeColor(
                    context,
                    isPrimary ? com.liskovsoft.smartyoutubetv2.tv.R.attr.m3SurfaceContainerHigh
                              : com.liskovsoft.smartyoutubetv2.tv.R.attr.m3SurfaceContainer,
                    com.liskovsoft.smartyoutubetv2.tv.R.color.m3_grey_surface_container_high);
            int focusedContainer = ViewUtil.getThemeColor(
                    context,
                    isPrimary ? com.liskovsoft.smartyoutubetv2.tv.R.attr.m3PrimaryContainer
                              : com.liskovsoft.smartyoutubetv2.tv.R.attr.m3SecondaryContainer,
                    com.liskovsoft.smartyoutubetv2.tv.R.color.m3_grey_primary_container);
            int outlineVariant = ViewUtil.getThemeColor(
                    context,
                    com.liskovsoft.smartyoutubetv2.tv.R.attr.m3OutlineVariant,
                    com.liskovsoft.smartyoutubetv2.tv.R.color.m3_grey_outline_variant);
            int outline = ViewUtil.getThemeColor(
                    context,
                    com.liskovsoft.smartyoutubetv2.tv.R.attr.m3Outline,
                    com.liskovsoft.smartyoutubetv2.tv.R.color.m3_grey_outline);

            int defaultFill = ColorUtils.setAlphaComponent(surfaceColor, isPrimary ? 190 : 165);
            int focusedFill = ColorUtils.setAlphaComponent(focusedContainer, 240);
            int defaultStroke = ColorUtils.setAlphaComponent(outlineVariant, 128);
            int focusedStroke = outline;

            float density = context.getResources().getDisplayMetrics().density;
            int defaultStrokePx = Math.max(1, Math.round(1f * density));
            int focusedStrokePx = Math.max(2, Math.round(2f * density));
            float pillRadius = context.getResources().getDimension(
                    com.liskovsoft.smartyoutubetv2.tv.R.dimen.m3_pill_corner_radius);

            vh.mFocusableView.setTag(
                    com.liskovsoft.smartyoutubetv2.tv.R.id.m3_focus_state_tag, null);
            M3Motion.applyCardFocusTransition(
                    vh.mFocusableView,
                    false,
                    focusedFill,
                    defaultFill,
                    focusedStroke,
                    defaultStroke,
                    focusedStrokePx,
                    defaultStrokePx,
                    pillRadius,
                    null);
            vh.mFocusableView.setOnFocusChangeListener(new FocusScaleListener(
                    vh.view,
                    vh.mFocusableView,
                    null,
                    defaultFill,
                    focusedFill,
                    defaultStroke,
                    focusedStroke,
                    defaultStrokePx,
                    focusedStrokePx,
                    pillRadius));
        }

        // Used inside: com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tweaks.ControlBarPresenter.ViewHolder.bindControlToAction()
        // Restore focus: com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tweaks.ControlBar.onRequestFocusInDescendants()
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            Action action = (Action) item;
            ActionViewHolder vh = (ActionViewHolder) viewHolder;

            vh.mIcon.setImageDrawable(action.getIcon());
            if (action instanceof PaddingAction) {
                int padding = ((PaddingAction) action).getPadding();
                if (padding > 0) {
                    vh.mIcon.setPadding(padding, padding, padding, padding);
                }
            }
            if (vh.mLabel != null) {
                if (action.getIcon() == null) {
                    vh.mLabel.setText(action.getLabel1());
                } else {
                    vh.mLabel.setText(null);
                }
            }
            CharSequence contentDescription = TextUtils.isEmpty(action.getLabel2())
                    ? action.getLabel1() : action.getLabel2();
            if (!TextUtils.equals(vh.mFocusableView.getContentDescription(), contentDescription)) {
                vh.mFocusableView.setContentDescription(contentDescription);
                vh.mFocusableView.sendAccessibilityEvent(
                        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);

                // MOD: enable control tooltips
                if (tooltipsEnabled) {
                    TooltipCompatHandler.setTooltipText(vh.mFocusableView, action.getLabel1());
                }
            }

            View.OnFocusChangeListener currentListener = vh.mFocusableView.getOnFocusChangeListener();
            if (!(currentListener instanceof FocusScaleListener)) {
                vh.mFocusableView.setOnFocusChangeListener(
                        new FocusScaleListener(vh.view, vh.mFocusableView, currentListener, 0, 0, 0, 0, 0, 0, 0f));
            } else {
                ((FocusScaleListener) currentListener).syncImmediate(vh.mFocusableView.hasFocus());
            }
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
            ActionViewHolder vh = (ActionViewHolder) viewHolder;
            vh.view.animate().cancel();
            vh.view.setScaleX(1.0f);
            vh.view.setScaleY(1.0f);
            M3Motion.cancelFocusAnimations(vh.mFocusableView);
            vh.mIcon.setImageDrawable(null);
            if (vh.mLabel != null) {
                vh.mLabel.setText(null);
            }
            vh.mFocusableView.setContentDescription(null);
        }

        @Override
        public void setOnClickListener(ViewHolder viewHolder,
                                       View.OnClickListener listener) {
            ((ActionViewHolder) viewHolder).mFocusableView.setOnClickListener(listener);
        }

        public void setOnLongClickListener(ViewHolder viewHolder,
                                       View.OnLongClickListener listener) {
            ((ActionViewHolder) viewHolder).mFocusableView.setOnLongClickListener(listener);
        }

        private static class FocusScaleListener implements View.OnFocusChangeListener {
            private static final float FOCUSED_SCALE = 1.10f;
            private final View mTargetView;
            private final View mSurfaceView;
            private final View.OnFocusChangeListener mDelegate;
            private final int mDefaultFill;
            private final int mFocusedFill;
            private final int mDefaultStroke;
            private final int mFocusedStroke;
            private final int mDefaultStrokePx;
            private final int mFocusedStrokePx;
            private final float mCornerRadius;
            private final boolean mAnimateSurface;

            FocusScaleListener(
                    View targetView,
                    View surfaceView,
                    View.OnFocusChangeListener delegate,
                    int defaultFill,
                    int focusedFill,
                    int defaultStroke,
                    int focusedStroke,
                    int defaultStrokePx,
                    int focusedStrokePx,
                    float cornerRadius) {
                mTargetView = targetView;
                mSurfaceView = surfaceView;
                mDelegate = delegate;
                mDefaultFill = defaultFill;
                mFocusedFill = focusedFill;
                mDefaultStroke = defaultStroke;
                mFocusedStroke = focusedStroke;
                mDefaultStrokePx = defaultStrokePx;
                mFocusedStrokePx = focusedStrokePx;
                mCornerRadius = cornerRadius;
                mAnimateSurface = cornerRadius > 0f;
            }

            void syncImmediate(boolean hasFocus) {
                float targetScale = hasFocus ? FOCUSED_SCALE : 1.0f;
                mTargetView.animate().cancel();
                mTargetView.setScaleX(targetScale);
                mTargetView.setScaleY(targetScale);
                if (mAnimateSurface && mSurfaceView != null) {
                    M3Motion.cancelFocusAnimations(mSurfaceView);
                    M3Motion.applyCardFocusTransition(
                            mSurfaceView,
                            hasFocus,
                            hasFocus ? mDefaultFill : mFocusedFill,
                            hasFocus ? mFocusedFill : mDefaultFill,
                            hasFocus ? mDefaultStroke : mFocusedStroke,
                            hasFocus ? mFocusedStroke : mDefaultStroke,
                            hasFocus ? mDefaultStrokePx : mFocusedStrokePx,
                            hasFocus ? mFocusedStrokePx : mDefaultStrokePx,
                            mCornerRadius,
                            null);
                }
            }

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mDelegate != null) {
                    mDelegate.onFocusChange(v, hasFocus);
                }
                M3Motion.animateFocusScale(mTargetView, hasFocus, FOCUSED_SCALE);
                if (mAnimateSurface && mSurfaceView != null) {
                    M3Motion.applyCardFocusTransition(
                            mSurfaceView,
                            hasFocus,
                            hasFocus ? mDefaultFill : mFocusedFill,
                            hasFocus ? mFocusedFill : mDefaultFill,
                            hasFocus ? mDefaultStroke : mFocusedStroke,
                            hasFocus ? mFocusedStroke : mDefaultStroke,
                            hasFocus ? mDefaultStrokePx : mFocusedStrokePx,
                            hasFocus ? mFocusedStrokePx : mDefaultStrokePx,
                            mCornerRadius,
                            null);
                }
            }
        }
    }
}
