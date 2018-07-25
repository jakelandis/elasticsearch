package org.elasticsearch.dissect;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DissectMatch {

    private final String appendSeparator;
    private final Map<String, String> results;
    private final Map<String, ReferenceResult> referenceResults;
    private final Map<String, AppendResult> appendResults;
    private int implicitAppendOrder = -1000;
    private final int maxMatches;
    private final int maxResults;
    private final int appendCount;
    private final int referenceCount;
    private int matches = 0;

    public DissectMatch(String appendSeparator, int maxMatches, int maxResults, int appendCount, int referenceCount) {
        this.maxMatches = maxMatches;
        this.maxResults = maxResults;
        this.appendCount = appendCount;
        this.referenceCount = referenceCount;
        this.appendSeparator = appendSeparator;
        results = new HashMap<>(maxMatches);
        referenceResults = referenceCount == 0 ? null : new HashMap<>(referenceCount);
        appendResults = appendCount == 0 ? null : new HashMap<>(appendCount);
    }

    public void add(DissectKey key, String value) {
        matches++;
        if (key.skip()) {
            return;
        }
        switch (key.getModifier()) {
            case NONE:
                results.put(key.getName(), value);
                break;
            case APPEND:
                appendResults.computeIfAbsent(key.getName(), k -> new AppendResult(appendSeparator)).addValue(value, implicitAppendOrder++);
                break;
            case APPEND_WITH_ORDER:
                appendResults.computeIfAbsent(key.getName(),
                    k -> new AppendResult(appendSeparator)).addValue(value, key.getAppendPosition());
                break;
            case FIELD_NAME:
                referenceResults.computeIfAbsent(key.getName(), k -> new ReferenceResult()).setKey(value);
                break;
            case FIELD_VALUE:
                referenceResults.computeIfAbsent(key.getName(), k -> new ReferenceResult()).setValue(value);
                break;
        }
    }

    public boolean fullyMatched() {
        return matches == maxMatches;
    }

    public boolean valid(Map<String, String> results) {
        return fullyMatched() && results.size() == maxResults;
    }

    public Map<String, String> getResults() {
        if (referenceCount > 0) {
            referenceResults.forEach((k, v) -> results.put(v.getKey(), v.getValue()));
        }
        if (appendCount > 0) {
            appendResults.forEach((k, v) -> results.put(k, v.getAppenedResult()));
        }
        return results;
    }


    class AppendResult {
        private final Set<AppendValue> values = new TreeSet<>();
        private final String appendSeperator;

        AppendResult(String appendSeperator) {
            this.appendSeperator = appendSeperator;
        }

        void addValue(String value, int order) {
            values.add(new AppendValue(value, order));
        }

        String getAppenedResult() {
            return values.stream().map(appendValue -> appendValue.getValue()).collect(Collectors.joining(appendSeperator));
        }
    }

    class AppendValue implements Comparable<AppendValue> {
        private final String value;
        private final int order;

        AppendValue(String value, int order) {
            this.value = value;
            this.order = order;
        }

        public String getValue() {
            return value;
        }

        public int getOrder() {
            return order;
        }

        @Override
        public int compareTo(AppendValue o) {
            return Integer.compare(this.order, o.getOrder());
        }
    }

    class ReferenceResult {

        private String key;

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        private String value;

        void setValue(String value) {
            this.value = value;
        }

        void setKey(String key) {
            this.key = key;
        }
    }
}
