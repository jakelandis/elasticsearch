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
public final class Hot implements ToXContentObject {
  @SuppressWarnings({"unchecked"})
  public static final ConstructingObjectParser<Hot, Void> PARSER = new ConstructingObjectParser<>(Hot.class.getName(), a -> new Hot(
  (Actions) a[0]));

  static {
    PARSER.declareObject(ConstructingObjectParser.constructorArg(), Actions.PARSER, new ParseField("actions"));
  }

  public final Actions actions;

  public Hot(Actions actions) {
    this.actions = actions;
  }

  @Override
  public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params)
      throws IOException {
    builder.startObject();
    builder.field("actions",actions);
    builder.endObject();
    return builder;
  }

  public static class Actions implements ToXContentObject {
    @SuppressWarnings({"unchecked"})
    public static final ConstructingObjectParser<Actions, Void> PARSER = new ConstructingObjectParser<>(Actions.class.getName(), a -> new Actions(
    (Rollover) a[0]));

    static {
      PARSER.declareObject(ConstructingObjectParser.constructorArg(), Rollover.PARSER, new ParseField("rollover"));
    }

    public final Rollover rollover;

    public Actions(Rollover rollover) {
      this.rollover = rollover;
    }

    @Override
    public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params)
        throws IOException {
      builder.startObject();
      builder.field("rollover",rollover);
      builder.endObject();
      return builder;
    }

    public static class Rollover implements ToXContentObject {
      @SuppressWarnings({"unchecked"})
      public static final ConstructingObjectParser<Rollover, Void> PARSER = new ConstructingObjectParser<>(Rollover.class.getName(), a -> new Rollover(
      (String) a[0],
      (String) a[1]));

      static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("max_age"));
        PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("max_size"));
      }

      public final String max_age;

      public final String max_size;

      public Rollover(String max_age, String max_size) {
        this.max_age = max_age;
        this.max_size = max_size;
      }

      @Override
      public XContentBuilder toXContent(final XContentBuilder builder,
          final ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("max_age",max_age);
        builder.field("max_size",max_size);
        builder.endObject();
        return builder;
      }
    }
  }
}
