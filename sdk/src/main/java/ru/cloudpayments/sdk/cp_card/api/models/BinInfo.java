package ru.cloudpayments.sdk.cp_card.api.models;

import org.json.JSONObject;

public final class BinInfo {

    private final JSONObject obj;

    public BinInfo(JSONObject obj) {
        this.obj = obj;
    }

    public String getLogoUrl() {
        return obj.optString("LogoUrl", null);
    }

    public String getBankName() {
        return obj.optString("BankName", null);
    }
}
