package ru.cloudpayments.sdk.three_ds;

import android.app.Activity;
import android.app.Dialog;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;


public final class ThreeDs {
    private ThreeDs() {}

    public static Dialog dialog(
        Activity activity,
        String acsUrl, String md, String paReq,
        @Nullable ThreeDSDialogListener listener
    ) {
        Dialog dialog = new Dialog(activity);
        dialog.setContentView(
            view(activity, acsUrl, md, paReq, new ThreeDSDialogListener() {
                @Override public void onAuthorizationCompleted(String md1, String paRes) {
                    if (listener != null) listener.onAuthorizationCompleted(md1, paRes);
                    dialog.dismiss();
                }
                @Override public void onAuthorizationFailed(String html) {
                    if (listener != null) listener.onAuthorizationFailed(html);
                    dialog.dismiss();
                }
            }),
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        );
        return dialog;
    }

    static WebView view(Activity activity, String acsUrl, String md, String paReq, ThreeDSDialogListener listener) {
        // view creation itself is left inside fragment in order to minimize diff
        return ThreeDsDialogFragment.view(activity, acsUrl, md, paReq, listener);
    }

}
