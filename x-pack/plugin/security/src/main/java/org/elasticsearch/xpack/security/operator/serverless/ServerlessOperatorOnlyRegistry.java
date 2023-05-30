/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.operator.serverless;

import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.Scope;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.xpack.security.operator.OperatorOnlyRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ServerlessOperatorOnlyRegistry implements OperatorOnlyRegistry {

    public OperatorOnlyRegistry.OperatorPrivilegesViolation check(String action, TransportRequest request) {
            //do nothing
            return null;
    }

    @Override
    public RestResponse checkRestFull(RestHandler restHandler) {
        System.out.println(String.format("checking maybe deny access for routes [%s] for scope [%s]", restHandler.routes().stream().map(RestHandler.Route::getPath).collect(Collectors.joining(",")), restHandler.getServerlessScope()));
        Scope scope = restHandler.getServerlessScope();
        Objects.requireNonNull(scope, "scope can not be null"); //upstream needs to guarantee this is never null
        if(Scope.PUBLIC.equals(scope)) {
            return null; //allow access
        }else {
            return new RestResponse(RestStatus.NOT_FOUND, "you must be an operator to call this rest handler");
        }
    }

    @Override
    public RestRequest checkRestPartial(RestHandler restHandler, RestRequest restRequest) {

        System.out.println("checking partially restricted operator privs...");
        if(restHandler.routes().stream().map(RestHandler.Route::getPath).anyMatch(p -> p.equals("/"))){
            Map<String, String> newParams = new HashMap<>(restRequest.getParams());
            newParams.put("restricted", "true");
            return RestRequest.updateWithCustomParams(restRequest, newParams);
        }

        return restRequest;

    }

}
