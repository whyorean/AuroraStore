package com.huawei.appmarket.service.externalservice.distribution.thirdsilentinstall;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;

import com.huawei.appgallery.coreservice.internal.framework.ipc.transport.data.BaseIPCRequest;
import com.huawei.appgallery.coreservice.internal.support.parcelable.AutoParcelable;
import com.huawei.appgallery.coreservice.internal.support.parcelable.EnableAutoParcel;

@Keep
public class SilentInstallRequest extends BaseIPCRequest {
    public static final Parcelable.Creator<SilentInstallRequest> CREATOR = new AutoParcelable.AutoCreator<>(SilentInstallRequest.class);

    public static final String METHOD = "method.requestSilentInstall";
    @EnableAutoParcel(1)
    private int sessionId;

    @Override
    public String getMethod() {
        return METHOD;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(sessionId);
    }

    public void readFromParcel(Parcel source) {
        this.sessionId = source.readInt();
    }

    public SilentInstallRequest() {
    }

    protected SilentInstallRequest(Parcel in) {
        this.sessionId = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
