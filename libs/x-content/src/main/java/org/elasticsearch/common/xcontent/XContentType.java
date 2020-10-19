/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.xcontent;

import org.elasticsearch.common.xcontent.cbor.CborXContent;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.common.xcontent.smile.SmileXContent;
import org.elasticsearch.common.xcontent.yaml.YamlXContent;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * The content type of {@link org.elasticsearch.common.xcontent.XContent}.
 */
public enum XContentType implements MediaType {

    /**
     * A JSON based content type.
     */
    JSON(0) {
        @Override
        public Set<String> mimeTypes() {
            return Set.of(
                "application/json",
                "application/vnd.elasticsearch+json",
                "application/x-ndjson",
                "application/vnd.elasticsearch+x-ndjson",
                "application/*"
            );
        }

        @Override
        public String shortName() {
            return "json";
        }

        @Override
        public XContent xContent() {
            return JsonXContent.jsonXContent;
        }
    },
    /**
     * The jackson based smile binary format. Fast and compact binary format.
     */
    SMILE(1) {
        @Override
        public Set<String> mimeTypes() {
            return Set.of(
                "application/smile",
                "application/vnd.elasticsearch+smile"
            );
        }

        @Override
        public String shortName() {
            return "smile";
        }

        @Override
        public XContent xContent() {
            return SmileXContent.smileXContent;
        }
    },
    /**
     * A YAML based content type.
     */
    YAML(2) {
        @Override
        public Set<String> mimeTypes() {
            return Set.of(
                "application/yaml",
                "application/vnd.elasticsearch+yaml"
            );
        }

        @Override
        public String shortName() {
            return "yaml";
        }

        public XContent xContent() {
            return YamlXContent.yamlXContent;
        }
    },
    /**
     * A CBOR based content type.
     */
    CBOR(3) {
        @Override
        public Set<String> mimeTypes() {
            return Set.of(
                "application/cbor",
                "application/vnd.elasticsearch+cbor"
            );
        }

        @Override
        public String shortName() {
            return "cbor";
        }

        @Override
        public XContent xContent() {
            return CborXContent.cborXContent;
        }
    };

    /**
     * Accepts either a format string, which is equivalent to {@link XContentType#shortName()} or a media type that optionally has
     * parameters and attempts to match the value to an {@link XContentType}. The comparisons are done in lower case format and this method
     * also supports a wildcard accept for {@code application/*}. This method can be used to parse the {@code Accept} HTTP header or a
     * format query string parameter. This method will return {@code null} if no match is found
     */
    public static XContentType fromMediaTypeOrFormat(String mediaType) {
        if (mediaType == null) {
            return null;
        }
        for (XContentType type : values()) {
            if (isSameMediaTypeOrFormatAs(mediaType, type)) {
                return type;
            }
        }
        final String lowercaseMediaType = mediaType.toLowerCase(Locale.ROOT);
        if (lowercaseMediaType.startsWith("application/*")) {
            return JSON;
        }

        return null;
    }

    /**
     * Attempts to match the given media type with the known {@link XContentType} values. This match is done in a case-insensitive manner.
     * The provided media type should not include any parameters. This method is suitable for parsing part of the {@code Content-Type}
     * HTTP header. This method will return {@code null} if no match is found
     */
    public static XContentType fromMediaType(String mediaType) {
        final String lowercaseMediaType = Objects.requireNonNull(mediaType, "mediaType cannot be null").toLowerCase(Locale.ROOT);
        for (XContentType type : values()) {
            if (type.mimeTypes().contains(lowercaseMediaType)) {
                return type;
            }
        }
        //TODO: don't allow null return type
        return null;
    }

    private static boolean isSameMediaTypeOrFormatAs(String stringType, XContentType type) {
   //TODO: fixme
        return false;
//        return type.mediaTypeWithoutParameters().equalsIgnoreCase(stringType) ||
//                stringType.toLowerCase(Locale.ROOT).startsWith(type.mediaTypeWithoutParameters().toLowerCase(Locale.ROOT) + ";") ||
//                type.shortName().equalsIgnoreCase(stringType);
    }

    private int index;

    XContentType(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public abstract XContent xContent();

}
