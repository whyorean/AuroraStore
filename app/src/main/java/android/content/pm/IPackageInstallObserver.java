/*
 * Copyright (C) 2015-2016 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import org.jetbrains.annotations.NotNull;

public interface IPackageInstallObserver extends IInterface {

    abstract class Stub extends Binder implements android.content.pm.IPackageInstallObserver {

        public Stub() {
            throw new RuntimeException("Stub!");
        }

        public static android.content.pm.IPackageInstallObserver asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }

        public IBinder asBinder() {
            throw new RuntimeException("Stub!");
        }

        public boolean onTransact(int code, @NotNull Parcel data, Parcel reply, int flags)
                throws RemoteException {
            throw new RuntimeException("Stub!");
        }
    }

    void packageInstalled(String packageName, int returnCode) throws RemoteException;
}