/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ContextPreservingActionListener;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.http.HttpAuthenticator;
import org.elasticsearch.http.HttpPreRequest;
import org.elasticsearch.xpack.security.authc.AuthenticationService;
import org.elasticsearch.xpack.security.rest.RemoteHostHeader;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class SecurityHttpAuthenticator implements HttpAuthenticator {

    private static final Logger logger = LogManager.getLogger(SecurityHttpAuthenticator.class);
    private final AuthenticationService authenticationService;
    private final ThreadContext threadContext;
    private final BiConsumer<HttpPreRequest, ThreadContext> perRequestThreadContext;
    private final BiConsumer<SSLEngine, ThreadContext> populateClientCertificate;

    public SecurityHttpAuthenticator(ThreadContext threadContext,
                                     AuthenticationService authenticationService,
                                     BiConsumer<HttpPreRequest, ThreadContext> perRequestThreadContext,
                                     BiConsumer<SSLEngine, ThreadContext> populateClientCertificate) {
        this.authenticationService = authenticationService;
        this.threadContext = threadContext;
        this.perRequestThreadContext = perRequestThreadContext;
        this.populateClientCertificate = populateClientCertificate;
    }


    public void authenticate(HttpPreRequest httpPreRequest,
                             InetSocketAddress remoteAddress, SSLEngine sslEngine, ActionListener<Supplier<ThreadContext.StoredContext>> listener) {

        try (ThreadContext.StoredContext ignore = threadContext.stashContext()) {
            assert threadContext.isDefaultContext();
            perRequestThreadContext.accept(httpPreRequest, threadContext);
            if(sslEngine != null) {
                populateClientCertificate.accept(sslEngine, threadContext);
            }
            RemoteHostHeader.process(remoteAddress, threadContext);
            authenticationService.authenticate(httpPreRequest, ActionListener.wrap(authentication -> {
                if (authentication == null) {
                    logger.trace("No authentication available for HTTP request [{}]", httpPreRequest.uri());
                } else {
                    logger.trace("Authenticated HTTP request [{}] as {}", httpPreRequest.uri(), authentication);
                }
                // Capture the now *authenticated* thread context and store it in a variable.
                // The captured authenticated context is going to be instated only while executing the associated request handler.
                listener.onResponse(threadContext.wrapRestorable(threadContext.newStoredContext()));
            }, listener::onFailure));
        }

    }
}
