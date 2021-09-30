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
        String acsUrl, String md, String paReq, String termUrl,
        @Nullable ThreeDSDialogListener listener
    ) {
        Dialog dialog = new Dialog(activity);
        dialog.setContentView(
            view(activity, acsUrl, md, paReq, termUrl, new ThreeDSDialogListener() {
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

    public static WebView view(Activity activity, String acsUrl, String md, String paReq, String termUrl, ThreeDSDialogListener listener) {
        // view creation itself is left inside fragment in order to minimize diff
        return ThreeDsDialogFragment.view(activity, acsUrl, md, paReq, termUrl, listener);
    }

    private static final byte[] PaReq = "PaReq=".getBytes();
    private static final byte[] AndMd = "&MD=".getBytes();
    private static final byte[] AndTermUrl = "&TermUrl=".getBytes();
    static byte[] requestBody(byte[] paReqVal, byte[] mdVal, byte[] termUrlVal) {
        byte[] params = new byte[
            PaReq.length + paReqVal.length + AndMd.length + mdVal.length + AndTermUrl.length + termUrlVal.length];
        int pos = 0;
        pos = append(params, pos, PaReq);
        pos = append(params, pos, paReqVal);
        pos = append(params, pos, AndMd);
        pos = append(params, pos, mdVal);
        pos = append(params, pos, AndTermUrl);
        pos = append(params, pos, termUrlVal);
        return params;
    }
    static int append(byte[] dst, int dstPos, byte[] src) {
        System.arraycopy(src, 0, dst, dstPos, src.length);
        return dstPos + src.length;
    }

}
