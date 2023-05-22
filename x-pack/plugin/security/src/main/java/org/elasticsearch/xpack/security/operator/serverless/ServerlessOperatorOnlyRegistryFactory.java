/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.operator.serverless;

import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.xpack.security.operator.DefaultOperatorOnlyRegistry;
import org.elasticsearch.xpack.security.operator.OperatorOnlyRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerlessOperatorOnlyRegistryFactory implements OperatorOnlyRegistry.Factory {
    private final AtomicBoolean created = new AtomicBoolean(false);

    @Override
    synchronized public OperatorOnlyRegistry create(ClusterSettings clusterSettings) {

        if (created.compareAndExchange(false, true) != false) {
            throw new RuntimeException("boom ... can't create multiple times");
        }
        return new ServerlessOperatorOnlyRegistry(clusterSettings);
    }

}
