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
public final class Cold implements ToXContentObject {
  @SuppressWarnings({"unchecked"})
  public static final ConstructingObjectParser<Cold, Void> PARSER = new ConstructingObjectParser<>(Cold.class.getName(), a -> new Cold(
  (String) a[0],
  (Actions) a[1]));

  static {
    PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("min_age"));
    PARSER.declareObject(ConstructingObjectParser.constructorArg(), Actions.PARSER, new ParseField("actions"));
  }

  public final String min_age;

  public final Actions actions;

  public Cold(String min_age, Actions actions) {
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
    (Allocate) a[0]));

    static {
      PARSER.declareObject(ConstructingObjectParser.constructorArg(), Allocate.PARSER, new ParseField("allocate"));
    }

    public final Allocate allocate;

    public Actions(Allocate allocate) {
      this.allocate = allocate;
    }

    @Override
    public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params)
        throws IOException {
      builder.startObject();
      builder.field("allocate",allocate);
      builder.endObject();
      return builder;
    }

    public static class Allocate implements ToXContentObject {
      @SuppressWarnings({"unchecked"})
      public static final ConstructingObjectParser<Allocate, Void> PARSER = new ConstructingObjectParser<>(Allocate.class.getName(), a -> new Allocate(
      (Require) a[0]));

      static {
        PARSER.declareObject(ConstructingObjectParser.constructorArg(), Require.PARSER, new ParseField("require"));
      }

      public final Require require;

      public Allocate(Require require) {
        this.require = require;
      }

      @Override
      public XContentBuilder toXContent(final XContentBuilder builder,
          final ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("require",require);
        builder.endObject();
        return builder;
      }

      public static class Require implements ToXContentObject {
        @SuppressWarnings({"unchecked"})
        public static final ConstructingObjectParser<Require, Void> PARSER = new ConstructingObjectParser<>(Require.class.getName(), a -> new Require(
        (String) a[0]));

        static {
          PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("type"));
        }

        public final String type;

        public Require(String type) {
          this.type = type;
        }

        @Override
        public XContentBuilder toXContent(final XContentBuilder builder,
            final ToXContent.Params params) throws IOException {
          builder.startObject();
          builder.field("type",type);
          builder.endObject();
          return builder;
        }
      }
    }
  }
}
