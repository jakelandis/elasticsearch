/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.rest.internal;

import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;

public interface RestRestrictions {

    //void if
    default RestResponse checkFullyRestricted(RestHandler restHandler, RestRequest request, ThreadContext threadContext) {
        return null;  //allow access;
    }

    default RestRequest checkPartiallyRestricted(RestHandler restHandler, RestRequest request, ThreadContext threadContext) {
        return request;
    }
}
