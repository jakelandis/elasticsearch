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

package org.elasticsearch.gradle.test.rest;


import com.fasterxml.jackson.databind.JsonNode;

import java.util.EnumSet;
import java.util.Objects;

public class Mutation {

    enum Action {
        REPLACE,
        REMOVE,
        ADD_BEFORE,
        ADD_AFTER;

        static Action fromString(String actionString) {
            return EnumSet.allOf(Action.class).stream().filter(a -> a.name().equalsIgnoreCase(actionString))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid action."));
        }
    }


    private final Action action;
    private final Location location;
    private final JsonNode jsonNode;

    public Mutation(Action action, String location, JsonNode jsonNode) {

        this.action = Objects.requireNonNull(action);
        this.jsonNode = jsonNode;
        String parts[] = location.split("\\.");
        if(parts.length != 2){
            throw new IllegalArgumentException("the compatible overrides must be of the format <name>.<position>, for example: match.0");
        }
        this.location = new Location(parts[0], Integer.parseInt(parts[1]));
    }

    public Action getAction() {
        return action;
    }



    public JsonNode getJsonNode() {
        return jsonNode;
    }

    public Location getLocation() {
        return location;
    }

    public static class Location
    {
        private final String key;
        private final int position;

        public Location(String key, int position) {
            this.key = key;
            this.position = position;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Location location = (Location) o;
            return position == location.position &&
                Objects.equals(key, location.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, position);
        }

        @Override
        public String toString() {
            return "Location{" +
                "key='" + key + '\'' +
                ", position=" + position +
                '}';
        }
    }
}

