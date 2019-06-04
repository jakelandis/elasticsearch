package org.elasticsearch.http.main.v8;

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

public class MainRestRequest8 extends MainRestRequest {

    public MainRestRequest8(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(GET, "/v8", this);
        controller.registerHandler(HEAD, "/v8", this);
        if (Version.CURRENT.major == 8) {
            controller.registerHandler(GET, "/", this);
            controller.registerHandler(HEAD, "/", this);
        }
    }


    @Override
    protected BytesRestResponse convertMainResponse(MainResponse response, RestRequest request, XContentBuilder builder) throws IOException {
        // Default to pretty printing, but allow ?pretty=false to disable
        if (request.hasParam("pretty") == false) {
            builder.prettyPrint().lfAtEnd();
        }

        MainResponseAdaptor8.toXContent(response, builder, request);
        return new BytesRestResponse(RestStatus.OK, builder);
    }

}
