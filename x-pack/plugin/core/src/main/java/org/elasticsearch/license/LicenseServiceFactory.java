/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.license;

import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;

import java.time.Clock;

public interface LicenseServiceFactory {

    /**
     * Creates a {@link LicenseService}
     */
    LicenseService create(
        Settings settings,
        ThreadPool threadPool,
        ClusterService clusterService,
        Clock clock,
        XPackLicenseState xPacklicenseState
    );
}
