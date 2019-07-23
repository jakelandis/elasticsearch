package org.elasticsearch.http.codegen;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;


public class FlatJsonObject implements Comparable<FlatJsonObject> {

    final String key;

    final int depth;
    final JsonNode jsonNode;


    public FlatJsonObject(String key, JsonNode jsonNode, int depth) {
        assert jsonNode.isObject();
        this.key = key;
        this.jsonNode = jsonNode;
        this.depth = depth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlatJsonObject that = (FlatJsonObject) o;
        return depth == that.depth &&
            Objects.equals(key, that.key) &&
            Objects.equals(jsonNode, that.jsonNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, depth, jsonNode);
    }

    @Override
    public String toString() {
        return "FlatJsonObject{" +
            "key='" + key + '\'' +
            ", depth=" + depth +
            ", jsonNode=" + jsonNode +
            '}';
    }

    @Override
    public int compareTo(FlatJsonObject o) {
        return Integer.valueOf(this.depth).compareTo(o.depth);
    }
}
