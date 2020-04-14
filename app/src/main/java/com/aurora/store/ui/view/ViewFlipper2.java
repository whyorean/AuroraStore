/*
 * Aurora Droid
 * Copyright (C) 2019-20, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Droid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Aurora Droid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Droid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ViewFlipper;

public class ViewFlipper2 extends ViewFlipper {

    public static final int PROGRESS = 0;
    public static final int DATA = 1;
    public static final int EMPTY = 2;
    public static final int ERROR = 3;
    public static final int NO_NETWORK = 4;

    public ViewFlipper2(Context context) {
        super(context);
        setDisplayedChild(PROGRESS);
    }

    public ViewFlipper2(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDisplayedChild(PROGRESS);
    }

    public void switchState(int state) {
        setDisplayedChild(state);
    }

}
