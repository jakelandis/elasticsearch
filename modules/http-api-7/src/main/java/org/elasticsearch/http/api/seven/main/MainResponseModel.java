package org.elasticsearch.http.api.seven.main;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.ConstructingObjectParser.constructorArg;

//Generated !! - well not yet ;)
final class MainResponseModel implements ToXContentObject {

    final String name;
    final String cluster_name;
    final String cluster_uuid;
    final Version version;
    private final String tagline = "You Know, for Search";

    static final ConstructingObjectParser<MainResponseModel, Void> PARSER = new ConstructingObjectParser<>(MainResponseModel.class.getName(),
        a -> new MainResponseModel(
            (String) a[0],
            (String) a[1],
            (String) a[2],
            (Version) a[3]
        ));

    static {
        PARSER.declareString(constructorArg(), new ParseField("name"));
        PARSER.declareString(constructorArg(), new ParseField("cluster_name"));
        PARSER.declareString(constructorArg(), new ParseField("cluster_uuid"));
        PARSER.declareString(constructorArg(), new ParseField("tagline"));
        PARSER.declareObject(constructorArg(), Version.PARSER, new ParseField("version"));
    }

    MainResponseModel(String name, String cluster_name, String cluster_uuid, Version version) {
        this.name = name;
        this.cluster_name = cluster_name;
        this.cluster_uuid = cluster_uuid;
        this.version = version;
    }
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("name", name);
        builder.field("cluster_name", cluster_name);
        builder.field("cluster_uuid", cluster_uuid);
        version.toXContent(builder, params);
        builder.field("tagline", tagline);
        builder.endObject();
        return builder;
    }

    static class Version implements ToXContentObject {
        final String number;
        final String build_flavor;
        final String build_type;
        final String build_hash;
        final String build_date;
        final boolean build_snapshot;
        final String lucene_version;
        final String minimum_wire_compatibility_version;
        final String minimum_index_compatibility_version;

        static final ConstructingObjectParser<Version, Void> PARSER = new ConstructingObjectParser<>(Version.class.getName(),
            a -> new Version(
                (String) a[0],
                (String) a[1],
                (String) a[2],
                (String) a[3],
                (String) a[4],
                (boolean) a[5],
                (String) a[6],
                (String) a[7],
                (String) a[8]

            ));

        static {
            PARSER.declareString(constructorArg(), new ParseField("number"));
            PARSER.declareString(constructorArg(), new ParseField("build_flavor"));
            PARSER.declareString(constructorArg(), new ParseField("build_hash"));
            PARSER.declareString(constructorArg(), new ParseField("build_date"));
            PARSER.declareBoolean(constructorArg(), new ParseField("build_snapshot"));
            PARSER.declareString(constructorArg(), new ParseField("lucene_version"));
            PARSER.declareString(constructorArg(), new ParseField("minimum_wire_compatibility_version"));
            PARSER.declareString(constructorArg(), new ParseField("minimum_index_compatibility_version"));

        }

        public Version(String number, String build_flavor, String build_type, String build_hash, String build_date, boolean build_snapshot, String lucene_version, String minimum_wire_compatibility_version, String minimum_index_compatibility_version) {
            this.number = number;
            this.build_flavor = build_flavor;
            this.build_type = build_type;
            this.build_hash = build_hash;
            this.build_date = build_date;
            this.build_snapshot = build_snapshot;
            this.lucene_version = lucene_version;
            this.minimum_wire_compatibility_version = minimum_wire_compatibility_version;
            this.minimum_index_compatibility_version = minimum_index_compatibility_version;
        }


        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject("version")
                .field("number", number)
                .field("build_flavor", build_flavor)
                .field("build_type", build_type)
                .field("build_hash", build_hash)
                .field("build_date", build_date)
                .field("build_snapshot", build_snapshot)
                .field("lucene_version", lucene_version)
                .field("minimum_wire_compatibility_version", minimum_wire_compatibility_version)
                .field("minimum_index_compatibility_version", minimum_index_compatibility_version)
                .endObject();
        }
    }

}
