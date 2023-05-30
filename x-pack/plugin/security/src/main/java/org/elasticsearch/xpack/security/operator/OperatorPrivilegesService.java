/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.operator;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.xpack.core.security.authc.Authentication;
import org.elasticsearch.xpack.core.security.authc.AuthenticationField;

public interface OperatorPrivilegesService {
    /**
     * Set a ThreadContext Header {@link AuthenticationField#PRIVILEGE_CATEGORY_KEY} if authentication
     * is an operator user.
     */
    void maybeMarkOperatorUser(Authentication authentication, ThreadContext threadContext);

    /**
     * Check whether the user is an operator and whether the request is an operator-only.
     *
     * @return An exception if user is an non-operator and the request is operator-only. Otherwise returns null.
     */
    ElasticsearchSecurityException check(
        Authentication authentication,
        String action,
        TransportRequest request,
        ThreadContext threadContext
    );

    RestResponse checkRestFull(RestHandler restHandler, ThreadContext threadContext);

    RestRequest checkRestPartial(RestHandler restHandler, RestRequest restRequest, ThreadContext threadContext);

    /**
     * When operator privileges are enabled, certain requests needs to be configured in a specific way
     * so that they respect operator only settings. For an example, the restore snapshot request
     * should not restore operator only states from the snapshot.
     * This method is where that requests are configured when necessary.
     */
    void maybeInterceptTransportRequest(ThreadContext threadContext, TransportRequest request);
}
