/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.operator;

import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.transport.TransportRequest;

public interface OperatorOnlyRegistry {

    /**
     * Check whether the given action and request qualify as operator-only. The method returns
     * null if the action+request is NOT operator-only. Other it returns a violation object
     * that contains the message for details. Generally this should be called from the
     * authorization service before allowing work to continue.
     * @return the OperatorPrivilegesViolation if failed the check, null otherwise
     */
    OperatorPrivilegesViolation check(String action, TransportRequest request);

    /**
     * Check whether the given REST handler qualify as operator-only. The method returns
     * null if the handler NOT operator-only. Other it returns a violation object
     * that contains the message for details. Generally this should be called before the
     * request is dispatched.
     * @return the OperatorPrivilegesViolation if failed the check, null otherwise
     */
    RestResponse checkRestFull(RestHandler restHandler);

    RestRequest checkRestPartial(RestHandler restHandler, RestRequest restRequest);

    @FunctionalInterface
    interface OperatorPrivilegesViolation {
        String message();
    }
}
