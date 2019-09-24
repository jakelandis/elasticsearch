/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.rest.action;

import org.elasticsearch.action.main.MainAction;
import org.elasticsearch.action.main.MainRequest;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.generated.AboutModel;
import org.elasticsearch.xcontent.generated.v7.AboutV7Model;


import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.HEAD;

public class RestMainAction extends BaseRestHandler {
    public RestMainAction(RestController controller) {
        controller.registerHandler(GET, "/", this);
        controller.registerHandler(HEAD, "/", this);
    }

    @Override
    public String getName() {
        return "main_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        boolean isVersion7 = request.isVersion7(); //need to consume this parameter early ... //TODO: make this not so ugly
        return channel -> client.execute(MainAction.INSTANCE, new MainRequest(), new RestBuilderListener<MainResponse>(channel) {
            @Override
            public RestResponse buildResponse(MainResponse mainResponse, XContentBuilder builder) throws Exception {

                return convertMainResponse(mainResponse, request, builder, isVersion7);
            }
        });
    }

    static BytesRestResponse convertMainResponse(MainResponse response, RestRequest request, XContentBuilder builder, boolean version7) throws IOException {
        // Default to pretty printing, but allow ?pretty=false to disable
        if (request.hasParam("pretty") == false) {
            builder.prettyPrint().lfAtEnd();
        }
        toXContent(request, response, builder,version7);
        return new BytesRestResponse(RestStatus.OK, builder);
    }

    @Override
    public boolean canTripCircuitBreaker() {
        return false;
    }


    private static void toXContent(RestRequest request, MainResponse mainResponse, XContentBuilder builder, boolean version7) throws IOException {
        if (version7) {
            toModelV7(mainResponse).toXContent(builder, request);
        } else {
            toModel(mainResponse).toXContent(builder, request);
        }
    }

    private static AboutV7Model toModelV7(MainResponse mainResponse) {
        return new AboutV7Model(
            mainResponse.getNodeName(), //name
            mainResponse.getClusterName().value(), //cluster_name
            mainResponse.getClusterUuid(), //cluster_uuid
            new AboutV7Model.Version(
                mainResponse.getBuild().getQualifiedVersion(), //number
                mainResponse.getBuild().flavor().displayName(), //build_flavor
                mainResponse.getBuild().type().displayName(), //build_type
                mainResponse.getBuild().hash(), //build_hash
                mainResponse.getBuild().date(), //build_date
                mainResponse.getBuild().isSnapshot(), //build_snapshot
                mainResponse.getVersion().luceneVersion.toString(), //lucene_version
                mainResponse.getVersion().minimumCompatibilityVersion().toString(), //minimum_wire_compatibility_version
                mainResponse.getVersion().minimumIndexCompatibilityVersion().toString() //minimum_index_compatibility_version
            ),
            "*****V7**** You know for versioning !!" //FIXME
        );
    }

    private static AboutModel toModel(MainResponse mainResponse) {
        return new AboutModel(
            mainResponse.getNodeName(), //name
            mainResponse.getClusterName().value(), //cluster_name
            mainResponse.getClusterUuid(), //cluster_uuid
            mainResponse.getBuild().getQualifiedVersion(), //number
            mainResponse.getBuild().flavor().displayName(), //build_flavor
            mainResponse.getBuild().type().displayName(), //build_type
            mainResponse.getBuild().hash(), //build_hash
            mainResponse.getBuild().date(), //build_date
            mainResponse.getBuild().isSnapshot(), //build_snapshot
            mainResponse.getVersion().luceneVersion.toString(), //lucene_version
            mainResponse.getVersion().minimumCompatibilityVersion().toString(), //minimum_wire_compatibility_version
            mainResponse.getVersion().minimumIndexCompatibilityVersion().toString() //minimum_index_compatibility_version
            , "*****V8**** You know for versioning !!" //FIXME
        );
    }


}
