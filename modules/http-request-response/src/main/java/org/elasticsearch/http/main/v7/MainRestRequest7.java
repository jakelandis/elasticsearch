package org.elasticsearch.http.main.v7;

import org.elasticsearch.Version;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.http.main.MainRestRequest;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.HEAD;

public class MainRestRequest7 extends MainRestRequest {

    public MainRestRequest7(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(GET, "/v7", this);
        controller.registerHandler(HEAD, "/v7", this);
        if (Version.CURRENT.major == 7) {
            controller.registerHandler(GET, "/", this);
            controller.registerHandler(HEAD, "/", this);
        }
    }


    protected BytesRestResponse convertMainResponse(MainResponse response, RestRequest request, XContentBuilder builder) throws IOException {
        // Default to pretty printing, but allow ?pretty=false to disable
        if (request.hasParam("pretty") == false) {
            builder.prettyPrint().lfAtEnd();
        }

        MainResponseAdaptor7.toXContent(response, builder, request);
        return new BytesRestResponse(RestStatus.OK, builder);
    }

}
