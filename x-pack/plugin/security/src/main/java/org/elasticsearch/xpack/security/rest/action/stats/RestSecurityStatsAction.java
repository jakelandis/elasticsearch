/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.rest.action.stats;

import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestActions;
import org.elasticsearch.xpack.security.action.stats.SecurityStatsAction;
import org.elasticsearch.xpack.security.action.stats.SecurityStatsRequest;
import org.elasticsearch.xpack.security.rest.action.SecurityBaseRestHandler;

import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.GET;

public class RestSecurityStatsAction  extends SecurityBaseRestHandler {

    public RestSecurityStatsAction(Settings settings, XPackLicenseState licenseState) {
        super(settings, licenseState);
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(GET, "/_security/stats")
                .build()
        );
    }

    @Override
    public String getName() {
        return "security_stats_action";
    }

    @Override
    public RestChannelConsumer innerPrepareRequest(RestRequest request, NodeClient client) {
//TODO: support filtering for which kind of stats as well as the node spec

        return channel -> client.execute(SecurityStatsAction.INSTANCE, new SecurityStatsRequest(),
            //or maybe  new RestToXContentListener<>(channel) ...depends on if we should honor the nodes specs ... probably should
            new RestActions.NodesResponseRestListener<>(channel));
    }
}
