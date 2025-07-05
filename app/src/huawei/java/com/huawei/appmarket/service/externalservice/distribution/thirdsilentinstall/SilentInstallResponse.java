package com.huawei.appmarket.service.externalservice.distribution.thirdsilentinstall;

import android.os.Parcelable;

import com.huawei.appgallery.coreservice.internal.framework.ipc.transport.data.BaseIPCResponse;
import com.huawei.appgallery.coreservice.internal.support.parcelable.AutoParcelable;
import com.huawei.appgallery.coreservice.internal.support.parcelable.EnableAutoParcel;

public class SilentInstallResponse extends BaseIPCResponse {

    public static final Parcelable.Creator<SilentInstallResponse> CREATOR = new AutoParcelable.AutoCreator<>(SilentInstallResponse.class);

    @EnableAutoParcel(1)
    private int result;

    public int getResult() {
        return this.result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}
