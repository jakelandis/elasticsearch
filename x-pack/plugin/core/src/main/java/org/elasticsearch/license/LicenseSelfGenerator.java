/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.license;

import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.core.Nullable;

import java.util.Objects;

public abstract class LicenseSelfGenerator extends ClusterStateUpdateTask {
    private static LicenseSelfGenerator instance;
    /**
     * Not intended for general usage.
     * @param instance An alternative self license generator.
     */
    public static void register(LicenseSelfGenerator instance) {
        if( LicenseSelfGenerator.instance != null ){
            throw new IllegalStateException("Can not register a LicenseSelfGenerator more than once");
        }
        LicenseSelfGenerator.instance = Objects.requireNonNull(instance);
    }

    public static @Nullable LicenseSelfGenerator get() {
        return instance;
    }
}
