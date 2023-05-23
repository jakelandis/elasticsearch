/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.rest.internal.serverless;

import org.elasticsearch.xpack.security.operator.OperatorPrivileges;
import org.elasticsearch.xpack.security.rest.internal.RestRestrictions;
import org.elasticsearch.xpack.security.rest.internal.RestRestrictionsFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerlessRestRestrictionsFactory implements RestRestrictionsFactory {

    AtomicBoolean created = new AtomicBoolean(false);
    @Override
    public synchronized RestRestrictions create(OperatorPrivileges.OperatorPrivilegesService operatorPrivilegesService) {
        if(created.getAndSet(true)){
            throw new RuntimeException("can not create multiple times");
        }
        return new ServerlessRestRestrictions(operatorPrivilegesService);
    }
}
