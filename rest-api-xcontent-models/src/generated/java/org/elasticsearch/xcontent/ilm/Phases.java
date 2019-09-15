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
public final class Phases implements ToXContentObject {
  @SuppressWarnings({"unchecked"})
  public static final ConstructingObjectParser<Phases, Void> PARSER = new ConstructingObjectParser<>(Phases.class.getName(), a -> new Phases(
  (Hot) a[0],
  (Warm) a[1],
  (Cold) a[2],
  (Delete) a[3]));

  static {
    PARSER.declareObject(ConstructingObjectParser.constructorArg(), Hot.PARSER, new ParseField("hot"));
    PARSER.declareObject(ConstructingObjectParser.constructorArg(), Warm.PARSER, new ParseField("warm"));
    PARSER.declareObject(ConstructingObjectParser.constructorArg(), Cold.PARSER, new ParseField("cold"));
    PARSER.declareObject(ConstructingObjectParser.constructorArg(), Delete.PARSER, new ParseField("delete"));
  }

  public final Hot hot;

  public final Warm warm;

  public final Cold cold;

  public final Delete delete;

  public Phases(Hot hot, Warm warm, Cold cold, Delete delete) {
    this.hot = hot;
    this.warm = warm;
    this.cold = cold;
    this.delete = delete;
  }

  @Override
  public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params)
      throws IOException {
    builder.startObject();
    builder.field("hot",hot);
    builder.field("warm",warm);
    builder.field("cold",cold);
    builder.field("delete",delete);
    builder.endObject();
    return builder;
  }
}
