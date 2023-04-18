/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.http.netty4;

import io.netty.handler.codec.http.HttpRequest;

import org.elasticsearch.http.HttpPreRequest;
import org.elasticsearch.rest.RestRequest;

import java.util.List;
import java.util.Map;

import static org.elasticsearch.http.netty4.Netty4HttpRequest.getHttpHeadersAsMap;
import static org.elasticsearch.http.netty4.Netty4HttpRequest.translateRequestMethod;


public class HttpRequestUtils {

    private HttpRequestUtils() {} //utility method
    /**
     * Translates the netty request internal type to a {@link HttpPreRequest} instance that code outside the network plugin has access to.
     */
    public static HttpPreRequest asHttpPreRequest(HttpRequest request) {
        return new HttpPreRequest() {

            @Override
            public RestRequest.Method method() {
                return translateRequestMethod(request.method());
            }

            @Override
            public String uri() {
                return request.uri();
            }

            @Override
            public Map<String, List<String>> getHeaders() {
                return getHttpHeadersAsMap(request.headers());
            }


        };
    }



}
