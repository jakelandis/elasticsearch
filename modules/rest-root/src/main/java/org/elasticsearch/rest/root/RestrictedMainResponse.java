/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.rest.root;

import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;

public class RestrictedMainResponse extends MainResponse implements ToXContentObject {

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        //TODO:  need to bind a "IVersion" interface to the constructor of TransportMainAction
        // so that we can call version.get() from the interface and get a different answer when running in serverless
        // this is mostly how the get license API works ... in serverless the get License api returns a different (but compatible) response
        // once we do that we can get this (in core ES) to say "serverless" without needing to expliclity reference anything in core
        // that says i am running in serverless
        builder.field("tagline", "You Know, for Search");
        builder.endObject();
        return builder;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        //FIXME: handle wire serialization
    }
}
