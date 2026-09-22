/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AC2DMUtil {

    public static Map<String, String> parseResponse(String response) {
        Map<String, String> keyValueMap = new HashMap<String, String>();
        StringTokenizer st = new StringTokenizer(response, "\n\r");
        while (st.hasMoreTokens()) {
            String[] keyValue = st.nextToken().split("=", 2);
            if (keyValue.length >= 2) {
                keyValueMap.put(keyValue[0], keyValue[1]);
            }
        }
        return keyValueMap;
    }

    public static Map<String, String> parseCookieString(String cookies) {
        Map<String, String> cookieList = new HashMap<>();
        Pattern cookiePattern = Pattern.compile("([^=]+)=([^;]*);?\\s?");
        Matcher matcher = cookiePattern.matcher(cookies);
        while (matcher.find()) {
            String cookieKey = matcher.group(1);
            String cookieValue = matcher.group(2);
            cookieList.put(cookieKey, cookieValue);
        }
        return cookieList;
    }
}
