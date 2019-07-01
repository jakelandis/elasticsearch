/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.security.action.token;

import org.elasticsearch.action.StreamableResponseActionType;

/**
 * ActionType for invalidating one or more tokens
 */
public final class InvalidateTokenAction extends StreamableResponseActionType<InvalidateTokenResponse> {

    public static final String NAME = "cluster:admin/xpack/security/token/invalidate";
    public static final InvalidateTokenAction INSTANCE = new InvalidateTokenAction();

    private InvalidateTokenAction() {
        super(NAME);
    }

    @Override
    public InvalidateTokenResponse newResponse() {
        return new InvalidateTokenResponse();
    }
}
