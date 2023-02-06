/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.license;

import org.elasticsearch.cluster.ClusterStateUpdateTask;

public abstract class LicenseSelfGenerator extends ClusterStateUpdateTask {
    private static LicenseSelfGenerator instance;
    /**
     * Call this to invert control to an alternative license generator. Not intended for general usage.
     * Registering this will effectively disable trial licenses.
     * @param instance The alternative license generator.
     */
    public static void register(LicenseSelfGenerator instance) {
        if( LicenseSelfGenerator.instance != null ){
            throw new RuntimeException("boom"); //TODO: better error
        }
        LicenseSelfGenerator.instance = instance;
    }

    public static LicenseSelfGenerator get() {
        return instance;
    }
}
