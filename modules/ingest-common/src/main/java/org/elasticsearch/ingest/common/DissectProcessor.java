package org.elasticsearch.ingest.common;


import org.elasticsearch.dissect.DissectParser;
import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.util.Map;

public final class DissectProcessor extends AbstractProcessor {

    public static final String TYPE = "dissect";
    private final String field;
    private final boolean ignoreMissing;
    private final String pattern;
    private final String appendSeparator;
    private final DissectParser dissectParser;

    DissectProcessor(String tag, String field, String pattern, String appendSeparator, boolean ignoreMissing) {
        super(tag);
        this.field = field;
        this.ignoreMissing = ignoreMissing;
        this.pattern = pattern;
        this.appendSeparator = appendSeparator;
        this.dissectParser = new DissectParser(pattern, appendSeparator);
    }

    String getField() {
        return field;
    }

    boolean isIgnoreMissing() {
        return ignoreMissing;
    }

    DissectParser getDissectParser() {
        return dissectParser;
    }

    String getPattern() {
        return pattern;
    }

    String getAppendSeparator() {
        return appendSeparator;
    }

    @Override
    public void execute(IngestDocument ingestDocument) {
        String input = ingestDocument.getFieldValue(field, String.class, ignoreMissing);
        if (input == null && ignoreMissing) {
            return;
        } else if (input == null) {
            throw new IllegalArgumentException("field [" + field + "] is null, cannot process it.");
        }
        dissectParser.parse(input).forEach(ingestDocument::setFieldValue);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory implements Processor.Factory {

        @Override
        public DissectProcessor create(Map<String, Processor.Factory> registry, String processorTag, Map<String, Object> config) {
            String field = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "field");
            String pattern = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "pattern");
            String appendSeparator = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "append_separator", "");
            boolean ignoreMissing = ConfigurationUtils.readBooleanProperty(TYPE, processorTag, config, "ignore_missing", false);
            return new DissectProcessor(processorTag, field, pattern, appendSeparator, ignoreMissing);
        }
    }
}
