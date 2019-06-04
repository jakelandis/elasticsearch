package org.elasticsearch.http.main;

import org.elasticsearch.action.main.MainAction;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.RestBuilderListener;

import java.io.IOException;

public abstract class MainRestRequest extends BaseRestHandler {

    protected MainRestRequest(Settings settings) {
        super(settings);
    }

    @Override
    public String getName() {
        return "main_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) {
        return channel -> client.execute(MainAction.INSTANCE, new org.elasticsearch.action.main.MainRequest(), new RestBuilderListener<>(channel) {
            @Override
            public RestResponse buildResponse(MainResponse mainResponse, XContentBuilder builder) throws Exception {
                return convertMainResponse(mainResponse, request, builder);
            }
        });
    }

    abstract protected BytesRestResponse convertMainResponse(MainResponse response, RestRequest request, XContentBuilder builder) throws IOException;

    @Override
    public boolean canTripCircuitBreaker() {
        return false;
    }
}
