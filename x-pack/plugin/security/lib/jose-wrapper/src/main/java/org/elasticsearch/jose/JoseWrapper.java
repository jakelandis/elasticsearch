/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.jose;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class JoseWrapper {

    // utility class
    private JoseWrapper() {}

    public static String getHeaderAsString(SignedJWT signedJWT) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> signedJWT.getHeader().toString());

    }

    public static String getClaimsSetAsString(JWTClaimsSet jwtClaimsSet) {
        return AccessController.doPrivileged((PrivilegedAction<String>) jwtClaimsSet::toString);
    }
}
