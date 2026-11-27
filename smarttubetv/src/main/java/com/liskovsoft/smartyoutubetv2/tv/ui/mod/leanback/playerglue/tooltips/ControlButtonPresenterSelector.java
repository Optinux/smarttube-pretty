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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.leanback.R;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.PaddingAction;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e.PlayPauseMorphDrawable;
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
        ControlButtonPresenter.FocusScaleListener mFocusListener;
        long mBoundActionId = View.NO_ID;
        int mBoundActionIndex = -1;

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
            int secondaryContainer = ViewUtil.getThemeColor(
                    context,
                    com.liskovsoft.smartyoutubetv2.tv.R.attr.m3SecondaryContainer,
                    com.liskovsoft.smartyoutubetv2.tv.R.color.m3_grey_secondary_container);
            int primary = ViewUtil.getThemeColor(
                    context,
                    com.liskovsoft.smartyoutubetv2.tv.R.attr.m3Primary,
                    com.liskovsoft.smartyoutubetv2.tv.R.color.m3_grey_primary);
            int onPrimary = ViewUtil.getThemeColor(
                    context,
                    com.liskovsoft.smartyoutubetv2.tv.R.attr.m3OnPrimary,
                    com.liskovsoft.smartyoutubetv2.tv.R.color.m3_grey_on_primary);
            int primaryContainer = ViewUtil.getThemeColor(
                    context,
                    com.liskovsoft.smartyoutubetv2.tv.R.attr.m3PrimaryContainer,
                    com.liskovsoft.smartyoutubetv2.tv.R.color.m3_grey_primary_container);
            int onPrimaryContainer = ViewUtil.getThemeColor(
                    context,
                    com.liskovsoft.smartyoutubetv2.tv.R.attr.m3OnPrimaryContainer,
                    com.liskovsoft.smartyoutubetv2.tv.R.color.m3_on_surface);
            int onSurface = ViewUtil.getThemeColor(
                    context,
                    com.liskovsoft.smartyoutubetv2.tv.R.attr.m3OnSurface,
                    com.liskovsoft.smartyoutubetv2.tv.R.color.m3_on_surface);
            int outline = ViewUtil.getThemeColor(
                    context,
                    com.liskovsoft.smartyoutubetv2.tv.R.attr.m3Outline,
                    com.liskovsoft.smartyoutubetv2.tv.R.color.m3_grey_outline);

            int defaultFill = ColorUtils.setAlphaComponent(surfaceColor, isPrimary ? 195 : 170);
            int activeDefaultFill = ColorUtils.setAlphaComponent(secondaryContainer, 224);
            int focusedFill = ColorUtils.setAlphaComponent(primaryContainer, 242);
            int heroDefaultFill = ColorUtils.setAlphaComponent(primaryContainer, 232);
            int heroFocusedFill = ColorUtils.setAlphaComponent(primary, 248);

            int focusedStrokePx = context.getResources().getDimensionPixelSize(
                    com.liskovsoft.smartyoutubetv2.tv.R.dimen.m3_focus_stroke_width);
            float pillRadius = context.getResources().getDimension(
                    com.liskovsoft.smartyoutubetv2.tv.R.dimen.m3_pill_corner_radius);
            float squircleRadius = context.getResources().getDimension(
                    com.liskovsoft.smartyoutubetv2.tv.R.dimen.m3_card_corner_radius);

            FocusScaleListener listener = new FocusScaleListener(
                    vh.view,
                    vh.mFocusableView,
                    vh.mIcon,
                    vh.mLabel,
                    defaultFill,
                    activeDefaultFill,
                    focusedFill,
                    outline,
                    onSurface,
                    onPrimaryContainer,
                    heroDefaultFill,
                    heroFocusedFill,
                    onPrimaryContainer,
                    onPrimary,
                    focusedStrokePx,
                    pillRadius,
                    squircleRadius);
            vh.mFocusListener = listener;
            vh.mFocusableView.setOnFocusChangeListener(listener);
            M3Motion.attachPressFeedback(vh.mFocusableView, M3Motion.FOCUS_SCALE_ICON);
            listener.syncImmediate(false);
        }

        // Used inside: com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tweaks.ControlBarPresenter.ViewHolder.bindControlToAction()
        // Restore focus: com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tweaks.ControlBar.onRequestFocusInDescendants()
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            Action action = (Action) item;
            ActionViewHolder vh = (ActionViewHolder) viewHolder;
            int actionIndex = action instanceof PlaybackControlsRow.MultiAction
                    ? ((PlaybackControlsRow.MultiAction) action).getIndex()
                    : 0;
            boolean sameActionStateChanged = vh.mBoundActionId == action.getId()
                    && vh.mBoundActionIndex != actionIndex
                    && ViewCompat.isAttachedToWindow(vh.mIcon)
                    && vh.mIcon.isShown();
            vh.mBoundActionId = action.getId();
            vh.mBoundActionIndex = actionIndex;

            if (action instanceof PlaybackControlsRow.PlayPauseAction) {
                Drawable currentDrawable = vh.mIcon.getDrawable();
                boolean isNew = !(currentDrawable instanceof PlayPauseMorphDrawable);
                PlayPauseMorphDrawable morphDrawable;
                if (isNew) {
                    morphDrawable = new PlayPauseMorphDrawable(vh.mIcon.getContext());
                    vh.mIcon.setImageDrawable(morphDrawable);
                } else {
                    morphDrawable = (PlayPauseMorphDrawable) currentDrawable;
                }
                boolean showPause = actionIndex == PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE;
                boolean animate = !isNew
                        && ViewCompat.isAttachedToWindow(vh.mIcon)
                        && vh.mIcon.isShown();
                morphDrawable.setShowPause(showPause, animate);
            } else {
                Drawable prevDrawable = vh.mIcon.getDrawable();
                if (prevDrawable instanceof PlayPauseMorphDrawable) {
                    ((PlayPauseMorphDrawable) prevDrawable).cancelAnimation();
                }
                vh.mIcon.setImageDrawable(action.getIcon());
                if (sameActionStateChanged) {
                    M3Motion.animateIconReveal(vh.mIcon);
                }
            }
            int actionPadding = 0;
            if (action instanceof PaddingAction) {
                actionPadding = ((PaddingAction) action).getPadding();
            }
            if (actionPadding > 0) {
                vh.mIcon.setPadding(actionPadding, actionPadding, actionPadding, actionPadding);
            } else {
                vh.mIcon.setPadding(0, 0, 0, 0);
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

            if (vh.mFocusListener != null) {
                boolean isHero = action instanceof PlaybackControlsRow.PlayPauseAction;
                boolean isActiveToggle = !isHero
                        && action instanceof PlaybackControlsRow.MultiAction
                        && ((PlaybackControlsRow.MultiAction) action).getIndex() > 0;
                boolean preserveIcon = actionPadding > 0 || isActiveToggle;
                vh.mFocusListener.configure(isHero, isActiveToggle, preserveIcon);
                vh.mFocusListener.syncImmediate(vh.mFocusableView.hasFocus());
            }
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
            ActionViewHolder vh = (ActionViewHolder) viewHolder;
            vh.view.animate().cancel();
            vh.view.setScaleX(1.0f);
            vh.view.setScaleY(1.0f);
            M3Motion.cancelFocusAnimations(vh.mFocusableView);
            vh.mBoundActionId = View.NO_ID;
            vh.mBoundActionIndex = -1;
            vh.mIcon.animate().cancel();
            vh.mIcon.setAlpha(1f);
            vh.mIcon.setScaleX(1f);
            vh.mIcon.setScaleY(1f);
            Drawable prevDrawable = vh.mIcon.getDrawable();
            if (prevDrawable instanceof PlayPauseMorphDrawable) {
                ((PlayPauseMorphDrawable) prevDrawable).cancelAnimation();
            }
            vh.mIcon.setImageDrawable(null);
            vh.mIcon.setPadding(0, 0, 0, 0);
            vh.mIcon.clearColorFilter();
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                vh.mIcon.setImageTintList((ColorStateList) null);
            }
            if (vh.mLabel != null) {
                vh.mLabel.setText(null);
            }
            TooltipCompatHandler.setTooltipText(vh.mFocusableView, null);
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

        static class FocusScaleListener implements View.OnFocusChangeListener {
            private static final float FOCUSED_SCALE = M3Motion.FOCUS_SCALE_ICON;
            private static final float HERO_FOCUSED_SCALE = M3Motion.FOCUS_SCALE_HERO_BUTTON;
            private final View mTargetView;
            private final View mSurfaceView;
            private final ImageView mIconView;
            private final TextView mLabelView;
            private final int mNormalDefaultFill;
            private final int mActiveDefaultFill;
            private final int mNormalFocusedFill;
            private final int mNormalFocusedStroke;
            private final int mNormalDefaultTint;
            private final int mNormalFocusedTint;
            private final int mHeroDefaultFill;
            private final int mHeroFocusedFill;
            private final int mHeroDefaultTint;
            private final int mHeroFocusedTint;
            private final int mFocusedStrokePx;
            private final float mPillRadius;
            private final float mSquircleRadius;
            private boolean mIsHero;
            private boolean mIsActiveToggle;
            private boolean mPreserveIconColor;

            FocusScaleListener(
                    View targetView,
                    View surfaceView,
                    ImageView iconView,
                    TextView labelView,
                    int normalDefaultFill,
                    int activeDefaultFill,
                    int normalFocusedFill,
                    int normalFocusedStroke,
                    int normalDefaultTint,
                    int normalFocusedTint,
                    int heroDefaultFill,
                    int heroFocusedFill,
                    int heroDefaultTint,
                    int heroFocusedTint,
                    int focusedStrokePx,
                    float pillRadius,
                    float squircleRadius) {
                mTargetView = targetView;
                mSurfaceView = surfaceView;
                mIconView = iconView;
                mLabelView = labelView;
                mNormalDefaultFill = normalDefaultFill;
                mActiveDefaultFill = activeDefaultFill;
                mNormalFocusedFill = normalFocusedFill;
                mNormalFocusedStroke = normalFocusedStroke;
                mNormalDefaultTint = normalDefaultTint;
                mNormalFocusedTint = normalFocusedTint;
                mHeroDefaultFill = heroDefaultFill;
                mHeroFocusedFill = heroFocusedFill;
                mHeroDefaultTint = heroDefaultTint;
                mHeroFocusedTint = heroFocusedTint;
                mFocusedStrokePx = focusedStrokePx;
                mPillRadius = pillRadius;
                mSquircleRadius = squircleRadius;
            }

            void configure(boolean isHero, boolean isActiveToggle, boolean preserveIconColor) {
                mIsHero = isHero;
                mIsActiveToggle = isActiveToggle;
                mPreserveIconColor = preserveIconColor;
            }

            void syncImmediate(boolean hasFocus) {
                float targetScale = hasFocus ? (mIsHero ? HERO_FOCUSED_SCALE : FOCUSED_SCALE) : 1.0f;
                mTargetView.animate().cancel();
                mTargetView.setScaleX(targetScale);
                mTargetView.setScaleY(targetScale);
                if (mPreserveIconColor && mIconView != null) {
                    mIconView.clearColorFilter();
                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                        mIconView.setImageTintList((ColorStateList) null);
                    }
                }
                if (mSurfaceView != null) {
                    M3Motion.cancelFocusAnimations(mSurfaceView);
                    applyTransition(hasFocus);
                }
            }

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                M3Motion.animateFocusScale(
                        mTargetView, hasFocus, mIsHero ? HERO_FOCUSED_SCALE : FOCUSED_SCALE);
                if (mSurfaceView != null) {
                    applyTransition(hasFocus);
                }
            }

            private void applyTransition(boolean hasFocus) {
                int defaultFill = mIsHero
                        ? mHeroDefaultFill
                        : (mIsActiveToggle ? mActiveDefaultFill : mNormalDefaultFill);
                int focusedFill = mIsHero ? mHeroFocusedFill : mNormalFocusedFill;
                int focusedStroke = mNormalFocusedStroke;
                int defaultTint = mIsHero ? mHeroDefaultTint : mNormalDefaultTint;
                int focusedTint = mIsHero ? mHeroFocusedTint : mNormalFocusedTint;
                float cornerRadius = mIsHero ? mSquircleRadius : mPillRadius;

                int fromTint = hasFocus ? defaultTint : focusedTint;
                int toTint = hasFocus ? focusedTint : defaultTint;

                M3Motion.applyCardFocusTransition(
                        mSurfaceView,
                        hasFocus,
                        hasFocus ? defaultFill : focusedFill,
                        hasFocus ? focusedFill : defaultFill,
                        hasFocus ? 0 : focusedStroke,
                        hasFocus ? focusedStroke : 0,
                        hasFocus ? 0 : mFocusedStrokePx,
                        hasFocus ? mFocusedStrokePx : 0,
                        cornerRadius,
                        (fraction, currentFill) -> {
                            int blendedTint = ColorUtils.blendARGB(fromTint, toTint, fraction);
                            if (!mPreserveIconColor) {
                                M3Motion.setImageTint(mIconView, blendedTint);
                            }
                            if (mLabelView != null) {
                                mLabelView.setTextColor(blendedTint);
                            }
                        });
            }
        }
    }
}
