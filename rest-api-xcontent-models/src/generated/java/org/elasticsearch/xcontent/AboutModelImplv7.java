package org.elasticsearch.xcontent;

import java.io.IOException;
import java.lang.Override;
import java.lang.SuppressWarnings;
import java.lang.Void;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

/**
 * GENERATED CODE - DO NOT MODIFY */
public final class AboutModelImplv7 implements ToXContentObject {
  @SuppressWarnings({"unchecked"})
  public static final ConstructingObjectParser<AboutModelImplv7, Void> PARSER = new ConstructingObjectParser<>(AboutModelImplv7.class.getName(), a -> new AboutModelImplv7(
  (String) a[0],
  (String) a[1],
  (String) a[2],
  (Version) a[3],
  (String) a[4]));

  static {
    PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("name"));
    PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("cluster_name"));
    PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("cluster_uuid"));
    PARSER.declareObject(ConstructingObjectParser.constructorArg(), Version.PARSER, new ParseField("version"));
    PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("tagline"));
  }

  public final String name;

  public final String cluster_name;

  public final String cluster_uuid;

  public final Version version;

  public final String tagline;

  public AboutModelImplv7(String name, String cluster_name, String cluster_uuid, Version version,
      String tagline) {
    this.name = name;
    this.cluster_name = cluster_name;
    this.cluster_uuid = cluster_uuid;
    this.version = version;
    this.tagline = tagline;
  }

  @Override
  public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params)
      throws IOException {
    builder.startObject();
    builder.field("name",name);
    builder.field("cluster_name",cluster_name);
    builder.field("cluster_uuid",cluster_uuid);
    builder.field("version",version);
    builder.field("tagline",tagline);
    builder.endObject();
    return builder;
  }

  public static class Version implements ToXContentObject {
    @SuppressWarnings({"unchecked"})
    public static final ConstructingObjectParser<Version, Void> PARSER = new ConstructingObjectParser<>(Version.class.getName(), a -> new Version(
    (String) a[0],
    (String) a[1],
    (String) a[2],
    (String) a[3],
    (String) a[4],
    (Boolean) a[5],
    (String) a[6],
    (String) a[7],
    (String) a[8]));

    static {
      PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("number"));
      PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("build_flavor"));
      PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("build_type"));
      PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("build_hash"));
      PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("build_date"));
      PARSER.declareBoolean(ConstructingObjectParser.constructorArg(), new ParseField("build_snapshot"));
      PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("lucene_version"));
      PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("minimum_wire_compatibility_version"));
      PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("minimum_index_compatibility_version"));
    }

    public final String number;

    public final String build_flavor;

    public final String build_type;

    public final String build_hash;

    public final String build_date;

    public final Boolean build_snapshot;

    public final String lucene_version;

    public final String minimum_wire_compatibility_version;

    public final String minimum_index_compatibility_version;

    public Version(String number, String build_flavor, String build_type, String build_hash,
        String build_date, Boolean build_snapshot, String lucene_version,
        String minimum_wire_compatibility_version, String minimum_index_compatibility_version) {
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
    public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params)
        throws IOException {
      builder.startObject();
      builder.field("number",number);
      builder.field("build_flavor",build_flavor);
      builder.field("build_type",build_type);
      builder.field("build_hash",build_hash);
      builder.field("build_date",build_date);
      builder.field("build_snapshot",build_snapshot);
      builder.field("lucene_version",lucene_version);
      builder.field("minimum_wire_compatibility_version",minimum_wire_compatibility_version);
      builder.field("minimum_index_compatibility_version",minimum_index_compatibility_version);
      builder.endObject();
      return builder;
    }
  }
}
