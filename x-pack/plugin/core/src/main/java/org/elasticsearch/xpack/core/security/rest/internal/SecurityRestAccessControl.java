/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.core.security.rest.internal;

import org.elasticsearch.rest.RestHandler;

public interface SecurityRestAccessControl {

    /**
     * @param restHandler The rest handler that will be called if allowed.
     * @return true if allowed, false otherwise
     */
    default boolean allow(RestHandler restHandler) {
        return true;
    }

}
