/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.node.reload.NodesReloadSecureSettingsResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.KeyStoreWrapper;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.SecuritySingleNodeTestCase;
import org.elasticsearch.transport.RemoteClusterService;
import org.elasticsearch.transport.TransportService;
import org.junit.BeforeClass;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ReloadRemoteClusterCredentialsIT extends SecuritySingleNodeTestCase {

    @BeforeClass
    public static void disableInFips() {
        // Reload secure settings with a password protected keystore is tested in ReloadSecureSettingsWithPasswordProtectedKeystoreRestIT
        assumeFalse(
            "Cannot run in FIPS mode since the keystore will be password protected and sending a password in the reload"
                + "settings api call, require TLS to be configured for the transport layer",
            inFipsJvm()
        );
    }

    public void testReloadRemoteClusterCredentials() throws Exception {
        final Environment environment = getInstanceFromNode(Environment.class);
        final KeyStoreWrapper keyStoreWrapper = KeyStoreWrapper.create();
        final String credentials = randomAlphaOfLength(42);
        keyStoreWrapper.setString("cluster.remote.my_remote_cluster.credentials", credentials.toCharArray());
        keyStoreWrapper.save(environment.configFile(), new char[0], false);

        final RemoteClusterService remoteClusterService = getInstanceFromNode(TransportService.class).getRemoteClusterService();
        assertThat(remoteClusterService.getRemoteClusterCredentialsManager().hasCredentials("my_remote_cluster"), is(false));

        successfulReloadCall();

        assertThat(remoteClusterService.getRemoteClusterCredentialsManager().resolveCredentials("my_remote_cluster"), equalTo(credentials));

    }

    private void successfulReloadCall() throws InterruptedException {
        final AtomicReference<AssertionError> reloadSettingsError = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final SecureString emptyPassword = randomBoolean() ? new SecureString(new char[0]) : null;
        clusterAdmin().prepareReloadSecureSettings()
            .setSecureStorePassword(emptyPassword)
            .setNodesIds(Strings.EMPTY_ARRAY)
            .execute(new ActionListener<>() {
                @Override
                public void onResponse(NodesReloadSecureSettingsResponse nodesReloadResponse) {
                    try {
                        assertThat(nodesReloadResponse, notNullValue());
                        final Map<String, NodesReloadSecureSettingsResponse.NodeResponse> nodesMap = nodesReloadResponse.getNodesMap();
                        assertThat(nodesMap.size(), equalTo(1));
                        for (final NodesReloadSecureSettingsResponse.NodeResponse nodeResponse : nodesReloadResponse.getNodes()) {
                            assertThat(nodeResponse.reloadException(), nullValue());
                        }
                    } catch (final AssertionError e) {
                        reloadSettingsError.set(e);
                    } finally {
                        latch.countDown();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    reloadSettingsError.set(new AssertionError("Nodes request failed", e));
                    latch.countDown();
                }
            });
        latch.await();
        if (reloadSettingsError.get() != null) {
            throw reloadSettingsError.get();
        }
    }

}
