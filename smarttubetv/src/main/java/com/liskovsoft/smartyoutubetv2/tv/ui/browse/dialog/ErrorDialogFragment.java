package com.liskovsoft.smartyoutubetv2.tv.ui.browse.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.app.BrowseSupportFragment.MainFragmentAdapter;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.CategoryEmptyError;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.SignInError;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.fragments.ErrorSupportFragment;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class ErrorDialogFragment extends ErrorSupportFragment implements BrowseSupportFragment.MainFragmentAdapterProvider {
    private static final boolean TRANSLUCENT = true;
    private static final int TIMER_DELAY = 1000;
    // Override style value 'lb_error_message_max_lines'
    private static final int NORMAL_MAX_LINES = 7;
    private static final int EXPANDED_MAX_LINES = 15;

    private final Handler mHandler = new Handler();
    private final ErrorFragmentData mDialogData;
    private final MainFragmentAdapter<Fragment> mMainFragmentAdapter =
            new MainFragmentAdapter<Fragment>(this) {
                @Override
                public void setEntranceTransitionState(boolean state) {
                    //setEntranceTransitionState(state);
                }
            };

    public ErrorDialogFragment() {
        // "could not find Fragment constructor" fix
        this(null);
    }

    public ErrorDialogFragment(ErrorFragmentData dialogData) {
        mDialogData = dialogData;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        setDialogContent();
    }

    private void setDialogContent() {
        if (mDialogData == null || getActivity() == null) {
            return;
        }

        Context context = getActivity();

        boolean isSadCloud = mDialogData instanceof CategoryEmptyError || mDialogData instanceof SignInError;
        if (isSadCloud) {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.lb_ic_sad_cloud));
        }

        setMessage(mDialogData.getMessage());

        TextView mTextView = (TextView) Helpers.getField(this, "mTextView");
        ImageView mImageView = (ImageView) Helpers.getField(this, "mImageView");
        if (mTextView != null) {
            if (mImageView != null) {
                mTextView.setMaxLines(mImageView.getVisibility() == View.GONE ? EXPANDED_MAX_LINES : NORMAL_MAX_LINES);
            }
            mTextView.setTextColor(ViewUtil.getThemeColor(
                    context, R.attr.m3OnSurface, R.color.m3_on_surface));
            mTextView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }

        if (mImageView != null) {
            if (isSadCloud) {
                mImageView.setColorFilter(ViewUtil.getThemeColor(
                        context, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant));
            } else {
                mImageView.clearColorFilter();
            }
        }

        Button mButton = (Button) Helpers.getField(this, "mButton");
        if (mDialogData.getActionText() != null) {
            setButtonText(mDialogData.getActionText());
            setButtonClickListener(v -> mDialogData.onAction());
            styleActionButton(context, mButton);
        } else if (mButton != null) {
            mButton.setVisibility(View.GONE);
        }
    }

    private void styleActionButton(Context context, Button button) {
        if (context == null || button == null) {
            return;
        }

        float density = context.getResources().getDisplayMetrics().density;
        int fill = ViewUtil.getThemeColor(
                context, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        int textColor = ViewUtil.getThemeColor(
                context, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);
        int outline = ViewUtil.getThemeColor(
                context, R.attr.m3Outline, R.color.m3_grey_outline);
        float pillRadius = context.getResources().getDimension(R.dimen.m3_pill_corner_radius);
        int strokeWidthPx = Math.max(1, Math.round(2f * density));

        int padH = Math.round(24 * density);
        int padV = Math.round(12 * density);
        button.setPaddingRelative(padH, padV, padH, padV);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        if (VERSION.SDK_INT >= 21) {
            button.setStateListAnimator(null);
        }
        button.setBackground(ViewUtil.createFocusSelectorDrawable(
                fill, fill, Color.TRANSPARENT, outline, 0, strokeWidthPx, pillRadius));
    }

    @Override
    public MainFragmentAdapter<Fragment> getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }
}
