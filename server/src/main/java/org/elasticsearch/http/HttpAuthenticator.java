/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.http;


import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.util.concurrent.ThreadContext;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.util.function.Supplier;


public interface HttpAuthenticator {

    void authenticate(HttpPreRequest httpPreRequest, InetSocketAddress remoteAddress, SSLEngine sslEngine, ActionListener<ThreadContext.StoredContext> listener);
}
