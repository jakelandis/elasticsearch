/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.http.netty4;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import org.elasticsearch.http.netty4.HttpRequestUtils.ValidatableHttpHeaders;
import org.elasticsearch.http.netty4.HttpRequestUtils.ValidatableHttpHeaders.ValidationResult;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public final class HttpHeadersValidatorTests extends ESTestCase {

    public void testRemoveHeaderPreservesValidationResult() {
        final ValidationResult validationResult = () -> {};
        final DefaultHttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        String header1 = "header1";
        String headerValue1 = "headerValue1";
        String header2 = "header2";
        String headerValue2 = "headerValue2";
        httpRequest.headers().add(header1, headerValue1);
        httpRequest.headers().add(header2, headerValue2);
        final DefaultHttpRequest validatableHttpRequest = (DefaultHttpRequest) HttpRequestUtils.wrapAsValidatableMessage(httpRequest);
        boolean validated = randomBoolean();
        if (validated) {
            ((ValidatableHttpHeaders) validatableHttpRequest.headers()).markAsSuccessfullyValidated(validationResult);
        }
        if (randomBoolean()) {
            validatableHttpRequest.headers().remove("header1");
            assertThat(validatableHttpRequest.headers().contains("header1"), is(false));
            assertThat(validatableHttpRequest.headers().contains("header2"), is(true));
        } else {
            validatableHttpRequest.headers().remove("header2");
            assertThat(validatableHttpRequest.headers().contains("header1"), is(true));
            assertThat(validatableHttpRequest.headers().contains("header2"), is(false));
        }
        if (validated) {
            assertThat(
                ((ValidatableHttpHeaders) validatableHttpRequest.headers()).validationResultContextSetOnce.get(),
                is(validationResult)
            );
        } else {
            assertThat(((ValidatableHttpHeaders) validatableHttpRequest.headers()).validationResultContextSetOnce.get(), nullValue());
        }
    }

    public void testCopyHeaderPreservesValidationResult() {
        final ValidationResult validationResult = () -> {};
        final DefaultHttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/uri");
        String header = "header";
        String headerValue = "headerValue";
        httpRequest.headers().add(header, headerValue);
        final DefaultHttpRequest validatableHttpRequest = (DefaultHttpRequest) HttpRequestUtils.wrapAsValidatableMessage(httpRequest);
        boolean validated = randomBoolean();
        if (validated) {
            ((ValidatableHttpHeaders) validatableHttpRequest.headers()).markAsSuccessfullyValidated(validationResult);
        }
        HttpHeaders httpHeadersCopy = ((ValidatableHttpHeaders) validatableHttpRequest.headers()).copy();
        if (validated) {
            assertThat(((ValidatableHttpHeaders) httpHeadersCopy).validationResultContextSetOnce.get(), is(validationResult));
        } else {
            assertThat(((ValidatableHttpHeaders) httpHeadersCopy).validationResultContextSetOnce.get(), nullValue());
        }
    }
}
