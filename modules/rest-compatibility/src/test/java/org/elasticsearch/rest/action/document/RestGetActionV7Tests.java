/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.rest.action.document;

import org.elasticsearch.compat.FakeCompatRestRequestBuilder;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.test.rest.FakeRestRequest;
import org.elasticsearch.test.rest.RestActionTestCase;
import org.junit.Before;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RestGetActionV7Tests extends RestActionTestCase {
    final String mimeType = "application/vnd.elasticsearch+json;compatible-with=7";
    final List<String> contentTypeHeader = Collections.singletonList(mimeType);

    @Before
    public void setUpAction() {
        controller().registerHandler(new RestGetActionV7());
    }

    public void testTypeInPathWithGet() {
        FakeRestRequest deprecatedRequest = new FakeCompatRestRequestBuilder(xContentRegistry()).withPath("/some_index/some_type/some_id")
            .withHeaders(Map.of("Content-Type", contentTypeHeader, "Accept", contentTypeHeader))
            .withMethod(RestRequest.Method.GET)
            .build();
        dispatchRequest(deprecatedRequest);
        assertWarnings(RestGetActionV7.TYPES_DEPRECATION_MESSAGE);
    }

    public void testTypeInPathWithHead() {
        FakeRestRequest deprecatedRequest = new FakeCompatRestRequestBuilder(xContentRegistry()).withPath("/some_index/some_type/some_id")
            .withMethod(RestRequest.Method.HEAD)
            .build();
        dispatchRequest(deprecatedRequest);
        assertWarnings(RestGetActionV7.TYPES_DEPRECATION_MESSAGE);
    }

}
