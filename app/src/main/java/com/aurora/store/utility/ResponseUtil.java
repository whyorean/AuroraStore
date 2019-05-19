/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Raccoon 4
 * Copyright 2019 Patrick Ahlbrecht
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

import com.dragons.aurora.playstoreapiv2.BulkDetailsResponse;
import com.dragons.aurora.playstoreapiv2.DeliveryResponse;
import com.dragons.aurora.playstoreapiv2.DetailsResponse;
import com.dragons.aurora.playstoreapiv2.ListResponse;
import com.dragons.aurora.playstoreapiv2.Payload;
import com.dragons.aurora.playstoreapiv2.ResponseWrapper;
import com.dragons.aurora.playstoreapiv2.SearchResponse;
import com.dragons.aurora.playstoreapiv2.TocResponse;
import com.dragons.aurora.playstoreapiv2.UploadDeviceConfigResponse;

public class ResponseUtil {

    public static Payload payload(ResponseWrapper responseWrapper) {
        if (responseWrapper != null && responseWrapper.hasPayload()) {
            return responseWrapper.getPayload();
        }
        return Payload.getDefaultInstance();
    }

    public static SearchResponse searchResponse(ResponseWrapper responseWrapper) {
        Payload payload = payload(responseWrapper);
        if (payload(responseWrapper).hasSearchResponse()) {
            return payload.getSearchResponse();
        }
        return SearchResponse.getDefaultInstance();
    }

    public static ListResponse listResponse(ResponseWrapper responseWrapper) {
        Payload payload = payload(responseWrapper);
        if (payload.hasListResponse()) {
            return payload.getListResponse();
        }
        return ListResponse.getDefaultInstance();
    }

    public static DeliveryResponse deliveryResponse(ResponseWrapper responseWrapper) {
        Payload payload = payload(responseWrapper);
        if (payload.hasDeliveryResponse()) {
            return payload.getDeliveryResponse();
        }
        return DeliveryResponse.getDefaultInstance();
    }

    public static BulkDetailsResponse bulkDetailsResponse(ResponseWrapper responseWrapper) {
        Payload payload = payload(responseWrapper);
        if (payload.hasBulkDetailsResponse()) {
            return payload.getBulkDetailsResponse();
        }
        return BulkDetailsResponse.getDefaultInstance();
    }

    public static DetailsResponse detailsResponse(ResponseWrapper responseWrapper) {
        Payload payload = payload(responseWrapper);
        if (payload.hasDetailsResponse()) {
            return payload.getDetailsResponse();
        }
        return DetailsResponse.getDefaultInstance();
    }

    public static TocResponse tocResponse(ResponseWrapper responseWrapper) {
        Payload payload = payload(responseWrapper);
        if (payload.hasTocResponse()) {
            return payload.getTocResponse();
        }
        return TocResponse.getDefaultInstance();
    }

    public static UploadDeviceConfigResponse uploadDeviceConfigResponse(ResponseWrapper responseWrapper) {
        Payload payload = payload(responseWrapper);
        if (payload.hasUploadDeviceConfigResponse()) {
            return payload.getUploadDeviceConfigResponse();
        }
        return UploadDeviceConfigResponse.getDefaultInstance();
    }
}
