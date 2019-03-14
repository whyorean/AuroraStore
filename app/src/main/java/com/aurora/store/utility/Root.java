/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.aurora.store.utility;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Root {
    private static final String TAG = "SAIRoot";

    private Process mSuProcess;
    private boolean mIsAcquired = true;
    private boolean mIsTerminated;

    private BufferedWriter mWriter;
    private BufferedReader mReader;
    private BufferedReader mErrorReader;

    public Root() {
        try {
            mSuProcess = Runtime.getRuntime().exec("su");
            mWriter = new BufferedWriter(new OutputStreamWriter(mSuProcess.getOutputStream()));
            mReader = new BufferedReader(new InputStreamReader(mSuProcess.getInputStream()));
            mErrorReader = new BufferedReader(new InputStreamReader(mSuProcess.getErrorStream()));
            exec("echo test");
        } catch (IOException e) {
            mIsAcquired = false;
            mIsTerminated = true;
            Log.w(TAG, "Unable to acquire root access: ");
            Log.w(TAG, e);
        }
    }

    public static boolean requestRoot() {
        try {
            Process root = Runtime.getRuntime().exec("su -c exit");
            root.waitFor();
            return root.exitValue() == 0;
        } catch (Exception e) {
            Log.w(TAG, "Unable to acquire root access: ");
            Log.w(TAG, e);
            return false;
        }
    }

    public String exec(String command) {
        try {
            StringBuilder sb = new StringBuilder();
            String breaker = "『BREAKER』";//Echoed after main command and used to determine when to stop reading from the stream
            mWriter.write(command + "\necho " + breaker + "\n");
            mWriter.flush();

            char[] buffer = new char[256];
            while (true) {
                sb.append(buffer, 0, mReader.read(buffer));

                int bi = sb.indexOf(breaker);
                if (bi != -1) {
                    sb.delete(bi, bi + breaker.length());
                    break;
                }
            }

            return sb.toString().trim();
        } catch (Exception e) {
            mIsAcquired = false;
            mIsTerminated = true;
            Log.w(TAG, "Unable execute command: ");
            Log.w(TAG, e);
        }

        return null;
    }

    public String readError() {
        try {
            StringBuilder sb = new StringBuilder();
            String breaker = "『BREAKER』";
            mWriter.write("echo " + breaker + " >&2\n");
            mWriter.flush();

            char[] buffer = new char[256];
            while (true) {
                sb.append(buffer, 0, mErrorReader.read(buffer));

                int bi = sb.indexOf(breaker);
                if (bi != -1) {
                    sb.delete(bi, bi + breaker.length());
                    break;
                }
            }

            return sb.toString().trim();
        } catch (Exception e) {
            mIsAcquired = false;
            mIsTerminated = true;
            Log.w(TAG, "Unable execute command: ");
            Log.w(TAG, e);
        }

        return null;
    }

    public void terminate() {
        if (mIsTerminated)
            return;

        mIsTerminated = true;
        mSuProcess.destroy();
    }

    public boolean isTerminated() {
        return mIsTerminated;
    }

    public boolean isAcquired() {
        return mIsAcquired;
    }
}
