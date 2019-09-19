/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ilm.action;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.support.master.AcknowledgedRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.xpack.core.ilm.LifecyclePolicy;

import java.io.IOException;
import java.util.Objects;

public class PutLifecycleAction extends ActionType<PutLifecycleAction.Response> {
    public static final PutLifecycleAction INSTANCE = new PutLifecycleAction();
    public static final String NAME = "cluster:admin/ilm/put";

    protected PutLifecycleAction() {
        super(NAME, PutLifecycleAction.Response::new);
    }

    public static class Response extends AcknowledgedResponse implements ToXContentObject {

        public Response(StreamInput in) throws IOException {
            super(in);
        }

        public Response(boolean acknowledged) {
            super(acknowledged);
        }
    }


    public static class Request extends AcknowledgedRequest<Request>  {

        private LifecyclePolicy policy;

        public Request(LifecyclePolicy policy) {
            this.policy = policy;
        }

        public Request(StreamInput in) throws IOException {
            super(in);
            policy = new LifecyclePolicy(in);
        }

        public Request() {
        }

        public LifecyclePolicy getPolicy() {
            return policy;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            policy.writeTo(out);
        }

        @Override
        public int hashCode() {
            return Objects.hash(policy);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            Request other = (Request) obj;
            return Objects.equals(policy, other.policy);
        }

        @Override
        public String toString() {
            //FIXME
            return "TODO";
            //return Strings.toString(this, true, true);
        }

    }

}
