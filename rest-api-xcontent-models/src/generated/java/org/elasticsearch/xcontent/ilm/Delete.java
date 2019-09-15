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
public final class Delete implements ToXContentObject {
  @SuppressWarnings({"unchecked"})
  public static final ConstructingObjectParser<Delete, Void> PARSER = new ConstructingObjectParser<>(Delete.class.getName(), a -> new Delete(
  (String) a[0],
  (Actions) a[1]));

  static {
    PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("min_age"));
    PARSER.declareObject(ConstructingObjectParser.constructorArg(), Actions.PARSER, new ParseField("actions"));
  }

  public final String min_age;

  public final Actions actions;

  public Delete(String min_age, Actions actions) {
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
    (Innerdelete) a[0]));

    static {
      PARSER.declareObject(ConstructingObjectParser.constructorArg(), Innerdelete.PARSER, new ParseField("delete"));
    }

    public final Innerdelete delete;

    public Actions(Innerdelete delete) {
      this.delete = delete;
    }

    @Override
    public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params)
        throws IOException {
      builder.startObject();
      builder.field("delete",delete);
      builder.endObject();
      return builder;
    }
  }
}
