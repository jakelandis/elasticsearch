package org.elasticsearch.xcontent;

import org.elasticsearch.common.xcontent.GenerateXContentParser;

@GenerateXContentParser(file="info.json", jPath="info/body", packageName="org.elasticsearch.xcontent.v8", className = "InfoParser")
public interface InfoParser {
}
