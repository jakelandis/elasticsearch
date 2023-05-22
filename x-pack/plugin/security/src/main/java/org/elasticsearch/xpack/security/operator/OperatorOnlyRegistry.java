/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.operator;

import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.transport.TransportRequest;

public interface OperatorOnlyRegistry {


    OperatorPrivilegesViolation checkTransportAction(String action, TransportRequest request);
    OperatorPrivilegesViolation checkRestAccess(RestHandler restHandler, ThreadContext threadContext);
    OperatorPrivilegesViolation checkClusterUpdateSettings(ClusterUpdateSettingsRequest request);



    interface Factory {
        OperatorOnlyRegistry create(ClusterSettings clusterSettings);
    }

    @FunctionalInterface
    interface OperatorPrivilegesViolation {
        String message();
    }
}
