/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.action.crosscluster;

import org.elasticsearch.action.ActionListener;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.transport.RemoteClusterService;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;

public class TransportUpdateRemoteClusterSecurity extends TransportAction<UpdateRemoteClusterSecurityAction.Request, ActionResponse> {

    private final RemoteClusterService remoteClusterService;
    private final ClusterService clusterService;

    @Inject
    public TransportUpdateRemoteClusterSecurity(
        TransportService transportService,
        ClusterService clusterService,
        ActionFilters actionFilters
    ) {
        super(UpdateRemoteClusterSecurityAction.NAME, actionFilters, transportService.getTaskManager());
        remoteClusterService = transportService.getRemoteClusterService();
        this.clusterService = clusterService;
    }

    @Override
    protected void doExecute(Task task, UpdateRemoteClusterSecurityAction.Request request, ActionListener<ActionResponse> listener) {
        System.out.println("** reference to remoteClusterService --> " + remoteClusterService.toString());
        System.out.println("** reference to clusterService --> " + clusterService.toString());
        System.out.println("** reference to inflight settings --> " + request.getSettings());

        // do some work...
        // if (remoteClusterService == null) {
        // final String msg = "remote cluster service unavailable during secure settings reload";
        // assert false : msg;
        // throw new IllegalStateException(msg);
        // }
        // // We need cluster settings to capture settings API updates;
        // final Settings persistentSettings = clusterService.state().metadata().persistentSettings();
        // final Settings transientSettings = clusterService.state().metadata().transientSettings();
        // remoteClusterService.updateRemoteClusterCredentials(
        // Settings.builder().put(settings, true).put(persistentSettings).put(transientSettings).build()
        // );

        listener.onResponse(new ActionResponse() {
            @Override
            public void writeTo(StreamOutput out) throws IOException {
                TransportAction.localOnly();
            }
        });
    }
}
