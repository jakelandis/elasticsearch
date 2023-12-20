/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.remotecluster;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.core.Strings;
import org.elasticsearch.test.cluster.ElasticsearchCluster;
import org.elasticsearch.test.cluster.util.resource.Resource;
import org.elasticsearch.test.junit.RunnableTestRuleAdapter;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.action.search.SearchResponse.LOCAL_CLUSTER_NAME_REPRESENTATION;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class RemoteClusterSecurityResolveClusterIT extends AbstractRemoteClusterSecurityTestCase {

    private static final AtomicReference<Map<String, Object>> API_KEY_MAP_REF = new AtomicReference<>();
    private static final AtomicReference<Map<String, Object>> REST_API_KEY_MAP_REF = new AtomicReference<>();
    private static final AtomicBoolean SSL_ENABLED_REF = new AtomicBoolean();
    private static final AtomicBoolean NODE1_RCS_SERVER_ENABLED = new AtomicBoolean();
    private static final AtomicBoolean NODE2_RCS_SERVER_ENABLED = new AtomicBoolean();
    private static final AtomicInteger INVALID_SECRET_LENGTH = new AtomicInteger();

    static {
        fulfillingCluster = ElasticsearchCluster.local()
            .name("fulfilling-cluster")
            .nodes(3)
            .apply(commonClusterConfig)
            .setting("remote_cluster.port", "0")
            .setting("xpack.security.remote_cluster_server.ssl.enabled", () -> String.valueOf(SSL_ENABLED_REF.get()))
            .setting("xpack.security.remote_cluster_server.ssl.key", "remote-cluster.key")
            .setting("xpack.security.remote_cluster_server.ssl.certificate", "remote-cluster.crt")
            .setting("xpack.security.authc.token.enabled", "true")
            .keystore("xpack.security.remote_cluster_server.ssl.secure_key_passphrase", "remote-cluster-password")
            .node(0, spec -> spec.setting("remote_cluster_server.enabled", "true"))
            .node(1, spec -> spec.setting("remote_cluster_server.enabled", () -> String.valueOf(NODE1_RCS_SERVER_ENABLED.get())))
            .node(2, spec -> spec.setting("remote_cluster_server.enabled", () -> String.valueOf(NODE2_RCS_SERVER_ENABLED.get())))
            .build();

        queryCluster = ElasticsearchCluster.local()
            .name("query-cluster")
            .apply(commonClusterConfig)
            .setting("xpack.security.remote_cluster_client.ssl.enabled", () -> String.valueOf(SSL_ENABLED_REF.get()))
            .setting("xpack.security.remote_cluster_client.ssl.certificate_authorities", "remote-cluster-ca.crt")
            .setting("xpack.security.authc.token.enabled", "true")
            .keystore("cluster.remote.my_remote_cluster.credentials", () -> {
                if (API_KEY_MAP_REF.get() == null) {
                    final Map<String, Object> apiKeyMap = createCrossClusterAccessApiKey("""
                        {
                          "search": [
                            {
                                "names": ["index*"]
                            }
                          ]
                        }""");
                    API_KEY_MAP_REF.set(apiKeyMap);
                }
                return (String) API_KEY_MAP_REF.get().get("encoded");
            })
            // Define a bogus API key for another remote cluster
            .keystore("cluster.remote.invalid_remote.credentials", randomEncodedApiKey())
            // Define remote with a REST API key to observe expected failure
            .keystore("cluster.remote.wrong_api_key_type.credentials", () -> {
                if (REST_API_KEY_MAP_REF.get() == null) {
                    initFulfillingClusterClient();
                    final var createApiKeyRequest = new Request("POST", "/_security/api_key");
                    createApiKeyRequest.setJsonEntity("""
                        {
                          "name": "rest_api_key"
                        }""");
                    try {
                        final Response createApiKeyResponse = performRequestWithAdminUser(fulfillingClusterClient, createApiKeyRequest);
                        assertOK(createApiKeyResponse);
                        REST_API_KEY_MAP_REF.set(responseAsMap(createApiKeyResponse));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                return (String) REST_API_KEY_MAP_REF.get().get("encoded");
            })
            // Define a remote with invalid API key secret length
            .keystore(
                "cluster.remote.invalid_secret_length.credentials",
                () -> Base64.getEncoder()
                    .encodeToString(
                        (UUIDs.base64UUID() + ":" + randomAlphaOfLength(INVALID_SECRET_LENGTH.get())).getBytes(StandardCharsets.UTF_8)
                    )
            )
            .rolesFile(Resource.fromClasspath("roles.yml"))
            .user(REMOTE_METRIC_USER, PASS.toString(), "read_remote_shared_metrics", false)
            .build();
    }

    @ClassRule
    // Use a RuleChain to ensure that fulfilling cluster is started before query cluster
    // `SSL_ENABLED_REF` is used to control the SSL-enabled setting on the test clusters
    // We set it here, since randomization methods are not available in the static initialize context above
    public static TestRule clusterRule = RuleChain.outerRule(new RunnableTestRuleAdapter(() -> {
        SSL_ENABLED_REF.set(usually());
        NODE1_RCS_SERVER_ENABLED.set(randomBoolean());
        NODE2_RCS_SERVER_ENABLED.set(randomBoolean());
        INVALID_SECRET_LENGTH.set(randomValueOtherThan(22, () -> randomIntBetween(0, 99)));
    })).around(fulfillingCluster).around(queryCluster);

    @SuppressWarnings("unchecked")
    public void testResolveCluster() throws Exception {
        configureRemoteCluster();

        // Query cluster -> add role for test user - do not give any privileges for remote_indices
        final var putRoleRequest = new Request("PUT", "/_security/role/" + REMOTE_SEARCH_ROLE);
        putRoleRequest.setJsonEntity("""
                {
                  "indices": [
                    {
                      "names": ["local_index"],
                      "privileges": ["read"]
                    }
                  ]
                }""");
        assertOK(adminClient().performRequest(putRoleRequest));

        // Query cluster -> create user and assign role
        final var putUserRequest = new Request("PUT", "/_security/user/" + REMOTE_SEARCH_USER);
        putUserRequest.setJsonEntity("""
                {
                  "password": "x-pack-test-password",
                  "roles" : ["remote_search"]
                }""");
        assertOK(adminClient().performRequest(putUserRequest));

        // Query cluster -> create test index
        final var indexDocRequest = new Request("POST", "/local_index/_doc?refresh=true");
        indexDocRequest.setJsonEntity("{\"local_foo\": \"local_bar\"}");
        assertOK(client().performRequest(indexDocRequest));

        // Fulfilling cluster -> create test indices
        final Request bulkRequest = new Request("POST", "/_bulk?refresh=true");
        bulkRequest.setJsonEntity(Strings.format("""
            { "index": { "_index": "index1" } }
            { "foo": "bar" }
            { "index": { "_index": "secretindex" } }
            { "bar": "foo" }
            """));
        assertOK(performRequestAgainstFulfillingCluster(bulkRequest));

        // Query cluster -> try to resolve local and remote star patterns (no access to remote cluster)
        final Request starResoloveRequest = new Request("GET", "_resolve/cluster/*,my_remote_cluster:*");
        Response response = performRequestWithRemoteSearchUser(starResoloveRequest);
        assertOK(response);
        Map<String, Object> responseMap = responseAsMap(response);
        assertLocalMatching(responseMap);

        Map<String, ?> remoteClusterResponse = (Map<String, ?>) responseMap.get("my_remote_cluster");
        assertThat((Boolean)remoteClusterResponse.get("connected"), equalTo(false));
        assertThat((String)remoteClusterResponse.get("error"), containsString("is unauthorized for user"));
        assertThat((String)remoteClusterResponse.get("error"), containsString("no remote indices privileges apply for the target cluster"));

        // Query cluster -> add remote privs to the user role
        final var updateRoleRequest = new Request("PUT", "/_security/role/" + REMOTE_SEARCH_ROLE);
        updateRoleRequest.setJsonEntity("""
                {
                  "indices": [
                    {
                      "names": ["local_index"],
                      "privileges": ["read"]
                    }
                  ],
                  "remote_indices": [
                    {
                      "names": ["index*"],
                      "privileges": ["read", "read_cross_cluster"],
                      "clusters": ["my_remote_cluster"]
                    }
                  ]
                }""");
        assertOK(adminClient().performRequest(updateRoleRequest));

        // Query cluster -> resolve local and remote with proper access
        response = performRequestWithRemoteSearchUser(starResoloveRequest);
        assertOK(response);
        responseMap = responseAsMap(response);
        assertLocalMatching(responseMap);
        assertRemoteMatching(responseMap);

        // Query cluster -> resolve local for local index without any local privilege
        final Request localOnly1 = new Request("GET", "_resolve/cluster/index1");
        ResponseException exception = expectThrows(ResponseException.class, () -> performRequestWithRemoteSearchUser(localOnly1));
        assertThat(exception.getResponse().getStatusLine().getStatusCode(), is(403));
        assertThat(exception.getMessage(), containsString("action [indices:admin/resolve/cluster] is unauthorized for user " +
            "[remote_search_user] with effective roles [remote_search] on indices [index1]"));

        // Query cluster -> resolve remote only for existing and privileged index
        final Request remoteOnly1 = new Request("GET", "_resolve/cluster/my_remote_cluster:index1");
        response = performRequestWithRemoteSearchUser(remoteOnly1);
        assertOK(response);
        responseMap = responseAsMap(response);
        assertThat(responseMap.get(LOCAL_CLUSTER_NAME_REPRESENTATION), nullValue());
        assertRemoteMatching(responseMap);

        // Query cluster -> resolve remote only for existing but non-privileged index
        final Request remoteOnly2 = new Request("GET", "_resolve/cluster/my_remote_cluster:secretindex");
        response = performRequestWithRemoteSearchUser(remoteOnly2);
        assertOK(response);
        responseMap = responseAsMap(response);
        assertThat(responseMap.get(LOCAL_CLUSTER_NAME_REPRESENTATION), nullValue());
        remoteClusterResponse = (Map<String, ?>) responseMap.get("my_remote_cluster");
        //TODO: is this the correct behavior ?
        // {my_remote_cluster={connected=false, skip_unavailable=false, error=org.elasticsearch.ElasticsearchSecurityException:
        // action [indices:admin/resolve/cluster] towards remote cluster is unauthorized for user [remote_search_user] with assigned
        // roles [remote_search] authenticated by API key id [Iez1iIwBfBlymu3gD8qN] of user [test_user] on indices [secretindex],
        // this action is granted by the index privileges [view_index_metadata,manage,read,read_cross_cluster,all]}}
        assertThat((Boolean)remoteClusterResponse.get("connected"), equalTo(false));
        assertThat((String)remoteClusterResponse.get("error"), containsString("is unauthorized for user"));
        assertThat((String)remoteClusterResponse.get("error"), containsString("with assigned roles [remote_search]"));
        assertThat((String)remoteClusterResponse.get("error"), containsString("on indices [secretindex]"));

        // Query cluster -> resolve remote only for non-existing and non-privileged index
        final Request remoteOnly3 = new Request("GET", "_resolve/cluster/my_remote_cluster:doesnotexist");
        response = performRequestWithRemoteSearchUser(remoteOnly3);
        assertOK(response);
        responseMap = responseAsMap(response);
        assertThat(responseMap.get(LOCAL_CLUSTER_NAME_REPRESENTATION), nullValue());
        remoteClusterResponse = (Map<String, ?>) responseMap.get("my_remote_cluster");
        //TODO: is this the correct behavior ? (it is good that the existence of the index does not change the response)
        // {my_remote_cluster={connected=false, skip_unavailable=false, error=org.elasticsearch.ElasticsearchSecurityException:
        // action [indices:admin/resolve/cluster] towards remote cluster is unauthorized for user [remote_search_user] with assigned
        // roles [remote_search] authenticated by API key id [i_EkiYwB-_Hx5jaPcbIy] of user [test_user] on indices [doesnotexist],
        // this action is granted by the index privileges [view_index_metadata,manage,read,read_cross_cluster,all]}}
        assertThat((Boolean)remoteClusterResponse.get("connected"), equalTo(false));
        assertThat((String)remoteClusterResponse.get("error"), containsString("is unauthorized for user"));
        assertThat((String)remoteClusterResponse.get("error"), containsString("with assigned roles [remote_search]"));
        assertThat((String)remoteClusterResponse.get("error"), containsString("on indices [doesnotexist]"));

        // Query cluster -> resolve remote only for non-existing but privileged (by index pattern) index
        final Request remoteOnly4 = new Request("GET", "_resolve/cluster/my_remote_cluster:index99");
        response = performRequestWithRemoteSearchUser(remoteOnly4);
        assertOK(response);
        responseMap = responseAsMap(response);
        assertThat(responseMap.get(LOCAL_CLUSTER_NAME_REPRESENTATION), nullValue());
        assertRemoteNotMatching(responseMap);

        // Query cluster -> resolve remote only for some existing/privileged, nonexisting/privileged, existing/nonprivileged
        final Request remoteOnly5 = new Request("GET",
            "_resolve/cluster/my_remote_cluster:index1,my_remote_cluster:secretindex,my_remote_cluster:index99");
        response = performRequestWithRemoteSearchUser(remoteOnly5);
        assertOK(response);
        responseMap = responseAsMap(response);
        assertThat(responseMap.get(LOCAL_CLUSTER_NAME_REPRESENTATION), nullValue());
        remoteClusterResponse = (Map<String, ?>) responseMap.get("my_remote_cluster");
        //TODO: same as above
        // {my_remote_cluster={connected=false, skip_unavailable=false, error=org.elasticsearch.ElasticsearchSecurityException:
        // action [indices:admin/resolve/cluster] towards remote cluster is unauthorized for user [remote_search_user] with assigned
        // roles [remote_search] authenticated by API key id [0kYLiYwB_vh01Ni_ok5A] of user [test_user] on indices [secretindex],
        // this action is granted by the index privileges [view_index_metadata,manage,read,read_cross_cluster,all]}}
        assertThat((Boolean)remoteClusterResponse.get("connected"), equalTo(false));
        assertThat((String)remoteClusterResponse.get("error"), containsString("is unauthorized for user"));
        assertThat((String)remoteClusterResponse.get("error"), containsString("with assigned roles [remote_search]"));
        assertThat((String)remoteClusterResponse.get("error"), containsString("on indices [secretindex]"));
    }

    private Response performRequestWithRemoteSearchUser(final Request request) throws IOException {
        request.setOptions(
            RequestOptions.DEFAULT.toBuilder().addHeader("Authorization", headerFromRandomAuthMethod(REMOTE_SEARCH_USER, PASS))
        );
        return client().performRequest(request);
    }

    @SuppressWarnings("unchecked")
    private void assertLocalMatching(Map<String, Object> responseMap){
        assertMatching((Map<String, Object>) responseMap.get(LOCAL_CLUSTER_NAME_REPRESENTATION), true);
    }

    @SuppressWarnings("unchecked")
    private void assertRemoteMatching(Map<String, Object> responseMap){
        assertMatching((Map<String, Object>) responseMap.get("my_remote_cluster"), true);
    }

    private void assertMatching(Map<String, Object> perClusterResponse, boolean matching) {
        assertThat((Boolean)perClusterResponse.get("connected"), equalTo(true));
        assertThat((Boolean)perClusterResponse.get("matching_indices"), equalTo(matching));
        assertThat(perClusterResponse.get("version"), notNullValue());
    }

    @SuppressWarnings("unchecked")
    private void assertRemoteNotMatching(Map<String, Object> responseMap){
        assertMatching((Map<String, Object>) responseMap.get("my_remote_cluster"), false);
    }
}
