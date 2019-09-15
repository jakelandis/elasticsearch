package org.elasticsearch.xcontent.ilm;

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
public final class Warm implements ToXContentObject {
  @SuppressWarnings({"unchecked"})
  public static final ConstructingObjectParser<Warm, Void> PARSER = new ConstructingObjectParser<>(Warm.class.getName(), a -> new Warm(
  (String) a[0],
  (Actions) a[1]));

  static {
    PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("min_age"));
    PARSER.declareObject(ConstructingObjectParser.constructorArg(), Actions.PARSER, new ParseField("actions"));
  }

  public final String min_age;

  public final Actions actions;

  public Warm(String min_age, Actions actions) {
    this.min_age = min_age;
    this.actions = actions;
  }

  @Override
  public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params)
      throws IOException {
    builder.startObject();
    builder.field("min_age",min_age);
    builder.field("actions",actions);
    builder.endObject();
    return builder;
  }

  public static class Actions implements ToXContentObject {
    @SuppressWarnings({"unchecked"})
    public static final ConstructingObjectParser<Actions, Void> PARSER = new ConstructingObjectParser<>(Actions.class.getName(), a -> new Actions(
    (Forcemerge) a[0],
    (Shrink) a[1],
    (Allocate) a[2]));

    static {
      PARSER.declareObject(ConstructingObjectParser.constructorArg(), Forcemerge.PARSER, new ParseField("forcemerge"));
      PARSER.declareObject(ConstructingObjectParser.constructorArg(), Shrink.PARSER, new ParseField("shrink"));
      PARSER.declareObject(ConstructingObjectParser.constructorArg(), Allocate.PARSER, new ParseField("allocate"));
    }

    public final Forcemerge forcemerge;

    public final Shrink shrink;

    public final Allocate allocate;

    public Actions(Forcemerge forcemerge, Shrink shrink, Allocate allocate) {
      this.forcemerge = forcemerge;
      this.shrink = shrink;
      this.allocate = allocate;
    }

    @Override
    public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params)
        throws IOException {
      builder.startObject();
      builder.field("forcemerge",forcemerge);
      builder.field("shrink",shrink);
      builder.field("allocate",allocate);
      builder.endObject();
      return builder;
    }

    public static class Forcemerge implements ToXContentObject {
      @SuppressWarnings({"unchecked"})
      public static final ConstructingObjectParser<Forcemerge, Void> PARSER = new ConstructingObjectParser<>(Forcemerge.class.getName(), a -> new Forcemerge(
      (Long) a[0]));

      static {
        PARSER.declareLong(ConstructingObjectParser.constructorArg(), new ParseField("max_num_segments"));
      }

      public final Long max_num_segments;

      public Forcemerge(Long max_num_segments) {
        this.max_num_segments = max_num_segments;
      }

      @Override
      public XContentBuilder toXContent(final XContentBuilder builder,
          final ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("max_num_segments",max_num_segments);
        builder.endObject();
        return builder;
      }
    }

    public static class Shrink implements ToXContentObject {
      @SuppressWarnings({"unchecked"})
      public static final ConstructingObjectParser<Shrink, Void> PARSER = new ConstructingObjectParser<>(Shrink.class.getName(), a -> new Shrink(
      (Long) a[0]));

      static {
        PARSER.declareLong(ConstructingObjectParser.constructorArg(), new ParseField("number_of_shards"));
      }

      public final Long number_of_shards;

      public Shrink(Long number_of_shards) {
        this.number_of_shards = number_of_shards;
      }

      @Override
      public XContentBuilder toXContent(final XContentBuilder builder,
          final ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("number_of_shards",number_of_shards);
        builder.endObject();
        return builder;
      }
    }

    public static class Allocate implements ToXContentObject {
      @SuppressWarnings({"unchecked"})
      public static final ConstructingObjectParser<Allocate, Void> PARSER = new ConstructingObjectParser<>(Allocate.class.getName(), a -> new Allocate(
      (Long) a[0]));

      static {
        PARSER.declareLong(ConstructingObjectParser.constructorArg(), new ParseField("number_of_replicas"));
      }

      public final Long number_of_replicas;

      public Allocate(Long number_of_replicas) {
        this.number_of_replicas = number_of_replicas;
      }

      @Override
      public XContentBuilder toXContent(final XContentBuilder builder,
          final ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("number_of_replicas",number_of_replicas);
        builder.endObject();
        return builder;
      }
    }
  }
}
