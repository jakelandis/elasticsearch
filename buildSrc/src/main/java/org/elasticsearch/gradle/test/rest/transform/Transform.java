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

package org.elasticsearch.gradle.test.rest.transform;


import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;

/**
 * Transforms a provided Json node as defined by the {@link TransformActions}
 */
@FunctionalInterface
public interface Transform {

    /**
     * Transform the Json structure per the given {@link TransformAction} This is intended to have side effects.
     * @param parentNode The parent of the node to transform.
     */
    void transform(ContainerNode<?> parentNode);

    /**
     * Find the node to transform via a {@link JsonPointer}
     */
    interface FindByLocation extends Transform  {
        JsonPointer location();
    }

    /**
     * Find the node to transform via a {@link JsonNode}
     */
    interface FindByMatch extends Transform {
        JsonNode nodeToFind();
    }
}
