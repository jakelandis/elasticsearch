/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.transport;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportRequest;

import java.util.Set;

public class FromUntrustedRemoteServerTransportFilter implements ServerTransportFilter {
    @Override
    public void inbound(String action, TransportRequest request, TransportChannel transportChannel, ActionListener<Void> listener) {

        //TODO: enforce API Key usage
        System.out.println("******* HERE **********");
        System.out.println("Handling action: " + action + " with request: " + request.toString() + " with channel type: "
            + transportChannel.getChannelType() + " from version: " + transportChannel.getVersion() + " with profile: "
            + transportChannel.getProfileName());

        if (request instanceof SearchRequest){
            SearchRequest searchRequest = (SearchRequest) request;
            if(Set.of(searchRequest.indices()).contains("goboom")) {
                listener.onFailure(new ElasticsearchSecurityException("kabooooooooom !!"));
                return;
            }
        }
        listener.onResponse(null);
    }
}
