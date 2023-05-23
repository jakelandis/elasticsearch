/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.rest.internal;

import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;

public interface RestRestrictions {

    //channel can retrun from here if access is denied
    default void maybeDenyAccess( RestHandler restHandler, RestChannel channel, ThreadContext threadContext) {
        //do nothing - i.e. allow access;
    }

    default Tuple<RestHandler, RestRequest> maybeRedirect(RestHandler handler, RestRequest request, ThreadContext threadContext) {
        return new Tuple<>(handler, request);
    }
}
