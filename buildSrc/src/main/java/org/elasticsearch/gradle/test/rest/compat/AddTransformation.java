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
import com.fasterxml.jackson.databind.node.ContainerNode;
import org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key.LOCATION;
import static org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key.OBJECT;

public class AddTransformation implements Transformation {

    private final List<Transform> additions = new ArrayList<>();

    public AddTransformation(List<TransformKeyValue> rawTransforms) {
        for (TransformKeyValue rawTransform : rawTransforms) {
            EnumSet<Key> actions = EnumSet.copyOf(rawTransform.getAllKeys());
            if (actions.stream().noneMatch(action -> action.equals(LOCATION))) {
                throw new IllegalStateException("'add' requires 'location' defined");
            }
            if (actions.stream().noneMatch(action -> action.equals(OBJECT))) {
                throw new IllegalStateException("'add' requires 'object' defined");
            }
            EnumSet<TransformKeyValue.Key> invalidActions = EnumSet.complementOf(EnumSet.of(LOCATION, OBJECT));
            Set<Key> invalid = actions.stream().filter(invalidActions::contains).collect(Collectors.toSet());
            if (invalid.isEmpty() == false) {
                throw new IllegalStateException("found invalid key(s) in 'add' definition [" +
                    invalid.stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")) + "]");
            }
            additions.add(new AddNodeAfterLocation(JsonPointer.compile(rawTransform.getLocation().asText()), rawTransform.getObject()));
        }
    }

    @Override
    public String toString() {
        return "AddAction{" +
            "additions=" + additions +
            '}';
    }

    @Override
    public List<Transform> getTransforms() {
        return Collections.unmodifiableList(additions);
    }

    static class AddNodeAfterLocation implements Transform.FindByLocation {
        private final JsonPointer location;
        private final JsonNode node;

        AddNodeAfterLocation(JsonPointer location, JsonNode node) {
            this.location = location;
            this.node = node;
        }

        @Override
        public JsonPointer location() {
            return location;
        }

        @Override
        public ContainerNode<?> transform(ContainerNode<?> input) {
            return null;
        }
    }
}
