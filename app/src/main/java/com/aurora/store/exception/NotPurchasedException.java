/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
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

package com.aurora.store.exception;

import java.io.IOException;

public class NotPurchasedException extends IOException {

    protected int code;

    public NotPurchasedException(String message, int code) {
        super(message);
        this.code = code;
    }

    public NotPurchasedException() {
        super("NotPurchasedException");
    }

    public NotPurchasedException(String message) {
        super(message);
    }

    public NotPurchasedException(String message, Throwable cause) {
        super(message, cause);
    }
}
