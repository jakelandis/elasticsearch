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
public final class IlmPutPolicyModelImpl implements ToXContentObject {
  @SuppressWarnings({"unchecked"})
  public static final ConstructingObjectParser<IlmPutPolicyModelImpl, Void> PARSER = new ConstructingObjectParser<>(IlmPutPolicyModelImpl.class.getName(), a -> new IlmPutPolicyModelImpl(
  (Policy) a[0]));

  static {
    PARSER.declareObject(ConstructingObjectParser.constructorArg(), Policy.PARSER, new ParseField("policy"));
  }

  public final Policy policy;

  public IlmPutPolicyModelImpl(Policy policy) {
    this.policy = policy;
  }

  @Override
  public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params)
      throws IOException {
    builder.startObject();
    builder.field("policy",policy);
    builder.endObject();
    return builder;
  }
}
