/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.action.crosscluster;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;

public class UpdateRemoteClusterSecurityAction extends ActionType<ActionResponse> {

    public static final String NAME = "cluster:monitor/foobar"; // TODO: use a real name
    public static final UpdateRemoteClusterSecurityAction INSTANCE = new UpdateRemoteClusterSecurityAction();

    private UpdateRemoteClusterSecurityAction() {
        super(NAME, Writeable.Reader.localOnly());
    }

    public static class Request extends ActionRequest {
        private final Settings settings;

        public Request(Settings settings) {
            this.settings = settings;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        public Settings getSettings() {
            return settings;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            TransportAction.localOnly();
        }
    }
}
