/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.transport;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportRequest;

public interface ServerTransportFilter {

    void inbound(String action, TransportRequest request, TransportChannel transportChannel, ActionListener<Void> listener);
}
