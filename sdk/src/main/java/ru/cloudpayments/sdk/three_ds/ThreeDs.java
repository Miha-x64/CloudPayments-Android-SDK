package ru.cloudpayments.sdk.three_ds;

import android.app.Activity;
import android.app.Dialog;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;

import static android.net.Uri.encode;
import static ru.cloudpayments.sdk.three_ds.ThreeDsDialogFragment.POST_BACK_URL;


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

    public static WebView view(Activity activity, String acsUrl, String md, String paReq, ThreeDSDialogListener listener) {
        // view creation itself is left inside fragment in order to minimize diff
        return ThreeDsDialogFragment.view(activity, acsUrl, md, paReq, listener);
    }

    private static final byte[] PaReq = "PaReq=".getBytes();
    private static final byte[] AndMd = "&MD=".getBytes();
    private static final byte[] AndTermUrl = ("&TermUrl=" + encode(POST_BACK_URL)).getBytes();
    static byte[] requestBody(byte[] paReqVal, byte[] mdVal) {
        byte[] params = new byte[PaReq.length + paReqVal.length + AndMd.length + mdVal.length + AndTermUrl.length];
        int pos = append(params, 0, PaReq);
        pos = append(params, pos, paReqVal);
        pos = append(params, pos, AndMd);
        pos = append(params, pos, mdVal);
        append(params, pos, AndTermUrl);
        return params;
    }
    static int append(byte[] dst, int dstPos, byte[] src) {
        System.arraycopy(src, 0, dst, dstPos, src.length);
        return dstPos + src.length;
    }

}
