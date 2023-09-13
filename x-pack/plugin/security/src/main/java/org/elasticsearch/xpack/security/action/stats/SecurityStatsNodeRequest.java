/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.action.stats;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.transport.TransportRequest;

import java.io.IOException;

public class SecurityStatsNodeRequest extends TransportRequest {

    public SecurityStatsNodeRequest(StreamInput in) throws IOException {
        super(in);
    }

}
