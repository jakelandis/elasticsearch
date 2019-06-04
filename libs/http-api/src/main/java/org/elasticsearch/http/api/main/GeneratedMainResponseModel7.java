package org.elasticsearch.http.api.main;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

//Generated !! - well not yet ;)
final public class GeneratedMainResponseModel7 implements ToXContentObject {

    final public String name;
    final public String cluster_name;
    final public String cluster_uuid;
    private final String tagline = "You Know, for Search";

    public static final ConstructingObjectParser<GeneratedMainResponseModel7, Void> PARSER = new ConstructingObjectParser<>(GeneratedMainResponseModel7.class.getName(),
        a -> new GeneratedMainResponseModel7(
            (String) a[0],
            (String) a[1],
            (String) a[2]
        ));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("name"));
        PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("cluster_name"));
        PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("cluster_uuid"));
        PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("tagline"));
    }

    public GeneratedMainResponseModel7(String name, String cluster_name, String cluster_uuid) {
        this.name = name;
        this.cluster_name = cluster_name;
        this.cluster_uuid = cluster_uuid;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("name", name);
        builder.field("cluster_name", cluster_name);
        builder.field("cluster_uuid", cluster_uuid);
        builder.field("tagline", tagline);
        builder.endObject();
        return builder;
    }



}
