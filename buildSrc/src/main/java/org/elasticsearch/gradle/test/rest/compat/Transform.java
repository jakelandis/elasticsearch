/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.gradle.test.rest.compat;


import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A Transform is a single instruction when applied in the context of {@link Transformation} defines a single specific action that
 * transforms the contents of a JsonNode. For example when executing in the context of an "add" {@link Transformation} the input
 * can be a JsonNode, and the output is a clone of that input, but with an additional key/value pair added. Implementations should
 * be free of side-effects.
 */
@FunctionalInterface
public interface Transform {
    /**
     * Perform the transformation. Implementations will likely pass in the parent of the Node to transform, create copy, then transform
     * the copy of the parent to return as the result of the function.
     */
    //TODO: change this to ContainerNode for input and output ??
    JsonNode transform(JsonNode input);

    /**
     * Find the node to transform via a given value. Value here is defined as the value in a key/value pairing.
     *
     * @param <T> the type of value to find
     */
    interface FindByValue<T> extends Transform {
        T valueToFind();
    }

    /**
     * Find the node to transform via a {@link JsonPointer}
     */
    interface FindByLocation extends Transform {
        JsonPointer location();
    }

    /**
     * Find the node to transform by a given {@link JsonNode}
     *
     * @param <T> the type of {@link JsonNode} to find
     */
    interface FindByNode<T extends JsonNode> extends Transform {
        T nodeToFind();
    }
}
