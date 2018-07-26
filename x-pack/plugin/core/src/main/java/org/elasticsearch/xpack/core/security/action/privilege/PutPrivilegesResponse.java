/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.security.action.privilege;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Response when adding one or more application privileges to the security index.
 * Returns a collection of the privileges that were created (by implication, any other privileges were updated).
 */
public final class PutPrivilegesResponse extends ActionResponse implements ToXContentObject {

    private Map<String, List<String>> created;

    PutPrivilegesResponse() {
        this(Collections.emptyMap());
    }

    public PutPrivilegesResponse(Map<String, List<String>> created) {
        this.created = Collections.unmodifiableMap(created);
    }

    /**
     * Get a list of privileges that were created (as opposed to updated)
     * @return A map from <em>Application Name</em> to a {@code List} of <em>privilege names</em>
     */
    public Map<String, List<String>> created() {
        return created;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject().field("created", created).endObject();
        return builder;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeMap(created, StreamOutput::writeString, StreamOutput::writeStringList);
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        this.created = Collections.unmodifiableMap(in.readMap(StreamInput::readString, si -> si.readList(StreamInput::readString)));
    }
}
