/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.example;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.test.rest.ESRestTestCase;

import java.io.IOException;

public class JavaRestTestExampleIT extends ESRestTestCase {


    public void testExample() throws IOException {
        final Request request = new Request("GET", "_example/foo");
        Response response = client().performRequest(request);
        response.getEntity().writeTo(System.out);
    }
}
