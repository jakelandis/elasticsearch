package org.elasticsearch.http.main.v7;

import org.elasticsearch.Build;
import org.elasticsearch.Version;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.http.api.main.GeneratedMainResponseModel7;

import java.io.IOException;

class MainResponseAdaptor7 {

    public static MainResponse fromXContent(XContentParser parser) {
        return fromModel(GeneratedMainResponseModel7.PARSER.apply(parser, null));
    }

    public static XContentBuilder toXContent(MainResponse mainResponse, XContentBuilder builder, ToXContent.Params params) throws IOException {
        return toModel(mainResponse).toXContent(builder, params);
    }

    //business logic
    private static GeneratedMainResponseModel7 toModel(MainResponse mainResponse) {
        return new GeneratedMainResponseModel7(
            mainResponse.getNodeName(), //name
            mainResponse.getClusterName().value(), //cluster_name
            mainResponse.getClusterUuid() //cluster_uuid

        );
    }


    //The buisness logic from plain POJO's to Response Object
    private static MainResponse fromModel(GeneratedMainResponseModel7 model) {
        return new MainResponse(
            model.name,
            Version.CURRENT, //Version is not supported in v7, this value is here to pacify the MainResponse Object
            new ClusterName(model.cluster_name),
            model.cluster_uuid,
            new Build( //Build is not supported in v7 ... in reality the MainResponse should have a constructor that supports this that is deprecrated.
               null,null,"","", false, ""
            )

        );
    }

    //Utility class
    private MainResponseAdaptor7() {}
}
