/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.rest.internal.serverless;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.Scope;
import org.elasticsearch.xpack.security.operator.OperatorPrivileges;
import org.elasticsearch.xpack.security.rest.internal.RestRestrictions;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ServerlessRestRestrictions implements RestRestrictions {

    private final OperatorPrivileges.OperatorPrivilegesService operatorPrivilegesService;

    public ServerlessRestRestrictions(OperatorPrivileges.OperatorPrivilegesService operatorPrivilegesService) {
        this.operatorPrivilegesService = operatorPrivilegesService;
    }

    //TODO: I can't actually close the channel here, so need to find another way to respond early preferably without throwing an exception
    @Override
    public void maybeDenyAccess(RestHandler restHandler, RestChannel channel, ThreadContext threadContext) {
        System.out.println(String.format("checking maybe deny access for routes [%s] for scope [%s]", restHandler.routes().stream().map(RestHandler.Route::getPath).collect(Collectors.joining(",")), restHandler.getServerlessScope()));

        //TODO: add a serverless feature flag to enable this
        Scope scope = restHandler.getServerlessScope();
        if(scope == null) {
            channel.sendResponse(new RestResponse(RestStatus.NOT_FOUND, "")); //no one has access
        } else {
            System.out.println("checking operator privs...");
            ElasticsearchSecurityException securityException = operatorPrivilegesService.checkRest(restHandler, threadContext);
            if(securityException != null) {
                System.out.println("found violation of operator privs...");
                channel.sendResponse(new RestResponse(RestStatus.NOT_FOUND, securityException.getMessage())); //must be an operator
            } else {
                System.out.println("did not find any violations ... allowing ...");
            }
        }
    }

    @Override
    public Tuple<RestHandler, RestRequest> maybeRedirect(RestHandler handler, RestRequest request, ThreadContext threadContext) {
        return new Tuple<>(handler, request);
    }
}
