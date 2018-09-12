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

package com.aurora.services;

import com.aurora.services.IPrivilegedCallback;

interface IPrivilegedService {

    boolean hasPrivilegedPermissions();

    /**
     * - Docs based on PackageManager.installPackage()
     * - Asynchronous (oneway) IPC calls!
     *
     * Install a package. Since this may take a little while, the result will
     * be posted back to the given callback. An installation will fail if the
     * package named in the package file's manifest is already installed, or if there's no space
     * available on the device.
     *
     * @param packageURI The location of the package file to install.  This can be a 'file:' or a
     * 'content:' URI.
     * @param flags - possible values: {@link #INSTALL_FORWARD_LOCK},
     * {@link #INSTALL_REPLACE_EXISTING}, {@link #INSTALL_ALLOW_TEST}.
     * @param installerPackageName Optional package name of the application that is performing the
     * installation. This identifies which market the package came from.
     * @param callback An callback to get notified when the package installation is
     * complete.
     */
    oneway void installPackage(in Uri packageURI, in int flags, in String installerPackageName,
                        in IPrivilegedCallback callback);


    /**
     * - Docs based on PackageManager.deletePackage()
     * - Asynchronous (oneway) IPC calls!
     *
     * Attempts to delete a package.  Since this may take a little while, the result will
     * be posted back to the given observer.  A deletion will fail if the
     * named package cannot be found, or if the named package is a "system package".
     *
     * @param packageName The name of the package to delete
     * @param flags - possible values: {@link #DELETE_KEEP_DATA},
     * {@link #DELETE_ALL_USERS}.
     * @param callback An callback to get notified when the package deletion is
     * complete.
     */
    oneway void deletePackage(in String packageName, in int flags, in IPrivilegedCallback callback);

}