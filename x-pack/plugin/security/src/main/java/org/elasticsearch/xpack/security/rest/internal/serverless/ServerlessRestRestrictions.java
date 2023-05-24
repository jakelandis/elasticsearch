/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.rest.internal.serverless;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.Scope;
import org.elasticsearch.xpack.security.operator.OperatorPrivileges;
import org.elasticsearch.xpack.security.rest.internal.RestRestrictions;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerlessRestRestrictions implements RestRestrictions {

    private final OperatorPrivileges.OperatorPrivilegesService operatorPrivilegesService;
    //addParamterForPath("/", "restricted=true")

    public ServerlessRestRestrictions(OperatorPrivileges.OperatorPrivilegesService operatorPrivilegesService) {
        this.operatorPrivilegesService = operatorPrivilegesService;
    }

    @Override
    public RestResponse checkFullyRestricted(RestHandler restHandler, RestRequest restRequest, ThreadContext threadContext) {
        System.out.println(String.format("checking maybe deny access for routes [%s] for scope [%s]", restHandler.routes().stream().map(RestHandler.Route::getPath).collect(Collectors.joining(",")), restHandler.getServerlessScope()));

        //TODO: add a serverless feature flag to enable this
        Scope scope = restHandler.getServerlessScope();
        if(scope == null) {
            return new RestResponse(RestStatus.NOT_FOUND, ""); //no one has access
        } else {
            System.out.println("checking fully restricted operator privs...");
            ElasticsearchSecurityException securityException = operatorPrivilegesService.checkRestFull(restHandler, threadContext);
            if(securityException != null) {
                System.out.println("found violation of operator privs...");
                return new RestResponse(RestStatus.NOT_FOUND, securityException.getMessage()); //must be an operator
            } else {
                System.out.println("did not find any violations ... allowing ...");
            }
        }
        return null;
    }

    @Override
    public RestRequest checkPartiallyRestricted(RestHandler restHandler, RestRequest restRequest, ThreadContext threadContext) {
        return operatorPrivilegesService.checkRestPartial(restHandler, restRequest, threadContext);
    }
}
