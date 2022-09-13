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

public class FromUntrustedRemoteServerTransportFilter implements ServerTransportFilter {
    @Override
    public void inbound(String action, TransportRequest request, TransportChannel transportChannel, ActionListener<Void> listener) {

        System.out.println("HERE!!");

    }
}
