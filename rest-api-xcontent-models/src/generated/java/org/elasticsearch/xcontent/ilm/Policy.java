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
public final class Policy implements ToXContentObject {
  @SuppressWarnings({"unchecked"})
  public static final ConstructingObjectParser<Policy, Void> PARSER = new ConstructingObjectParser<>(Policy.class.getName(), a -> new Policy(
  (Phases) a[0]));

  static {
    PARSER.declareObject(ConstructingObjectParser.constructorArg(), Phases.PARSER, new ParseField("phases"));
  }

  public final Phases phases;

  public Policy(Phases phases) {
    this.phases = phases;
  }

  @Override
  public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params)
      throws IOException {
    builder.startObject();
    builder.field("phases",phases);
    builder.endObject();
    return builder;
  }
}
