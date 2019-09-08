package org.elasticsearch.action.main;

import org.elasticsearch.Build;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xcontent.annotations.response.generated.GeneratedXContentInfoResponseParser;

import java.io.IOException;

public class MainResponseXContent {
    public static MainResponse fromXContent(XContentParser parser) {
        return fromParser(GeneratedXContentInfoResponseParser.PARSER.apply(parser, null));
    }

    public static XContentBuilder toXContent(MainResponse mainResponse, XContentBuilder builder, ToXContent.Params params) throws IOException {
        return toParser(mainResponse).toXContent(builder, params);
    }

    //business logic
    private static GeneratedXContentInfoResponseParser toParser(MainResponse mainResponse) {
        return new GeneratedXContentInfoResponseParser(
            mainResponse.getNodeName(), //name
            mainResponse.getClusterName().value(), //cluster_name
            mainResponse.getClusterUuid(), //cluster_uuid
            new GeneratedXContentInfoResponseParser.Version(
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
            "You know for versioning !!" //FIXME
        );
    }


    //The buisness logic from plain POJO's to Response Object
    private static MainResponse fromParser(GeneratedXContentInfoResponseParser model) {
        return new MainResponse(
            model.name,
            Version.fromString(model.version.number),
            new ClusterName(model.cluster_name),
            model.cluster_uuid,
            new Build(
                /*
                 * Be lenient when reading on the wire, the enumeration values from other versions might be different than what
                 * we know.
                 */
                model.version.build_flavor == null ? Build.Flavor.UNKNOWN : Build.Flavor.fromDisplayName(model.version.build_flavor, false),
                model.version.build_type == null ? Build.Type.UNKNOWN : Build.Type.fromDisplayName(model.version.build_type, false),
                model.version.build_hash,
                model.version.build_date,
                model.version.build_snapshot,
                model.version.number
            )
        );
    }

    //Utility class
    private MainResponseXContent() {}
}
