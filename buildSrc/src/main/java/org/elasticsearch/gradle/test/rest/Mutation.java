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
import java.util.Optional;

public class Mutation implements Comparable<Mutation>{


    @Override
    public int compareTo(Mutation o) {
        //if sorting ensure that DO is listed first
        if(ExecutableSection.DO.equals(o.getLocation().section)){
            return  -1; //ensuring that all do's come before any others
        }
        return 0;
    }

    //the action to perform
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

    //These are a mirror of org.elasticsearch.test.rest.yaml.section.ExecutableSection
    // these are duplicated here for validation (and we can't reference that class directly)
    //These are the supported sections that we can locate. For example match.1, or lt.0
    enum ExecutableSection {
        DO,
        SET,
        TRANSFORM_AND_SET,
        MATCH,
        IS_TRUE,
        IS_FALSE,
        GT,
        GTE,
        LT,
        LTE,
        CONTAINS,
        LENGTH;

        static ExecutableSection fromString(String section) {
            return EnumSet.allOf(ExecutableSection.class).stream().filter(a -> a.name().equalsIgnoreCase(section))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid section."));
        }
    }


    // These are the supported children for do that we can directly reference. Any
    // for example, do.0.catch, do.1.warnings
    enum DoSectionSub {
        CATCH,
        HEADERS,
        WARNINGS,
        ALLOWED_WARNINGS;

        static Optional<DoSectionSub> fromString(String section) {
            return EnumSet.allOf(DoSectionSub.class).stream().filter(a -> a.name().equalsIgnoreCase(section))
                .findFirst();
        }
    }

    private final Action action;
    private final Location location;
    private final JsonNode jsonNode;

    public Mutation(Action action, String location, JsonNode jsonNode) {

        this.action = Objects.requireNonNull(action);
        this.jsonNode = jsonNode;
        String parts[] = location.split("\\.");
        if(parts.length < 2){
            //TODO: clean this up and source from the enumerations
            throw new IllegalArgumentException("the compatible overrides must be of the format: " +
                "[do|set|transform_and_set|match|is_true|is_false|gt|gte|lt|lte|contains|length].[0-9]*, for example: match.0; " +
                "or may be do.[0-9]*.[catch|headers|warnings|allowed_warnings], for example do.0.catch");
        }
        //TODO: actually implement the doSection
        this.location = new Location(ExecutableSection.fromString(parts[0]), Integer.parseInt(parts[1]), parts.length == 3 ? DoSectionSub.fromString(parts[2]).get() : null);
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

    @Override
    public String toString() {
        return "Mutation{" +
            "action=" + action +
            ", location=" + location +
            ", jsonNode=" + jsonNode +
            '}';
    }

    public static class Location implements Comparable<Location>
    {
        private final ExecutableSection section;
        private final DoSectionSub doSectionSub;
        private final int position;

        public Location(ExecutableSection section, int position){
            this(section, position, null);
        }

        public Location(ExecutableSection section, int position, DoSectionSub doSectionSub) {
            if(doSectionSub != null && ExecutableSection.DO.equals(section) == false){
                throw new IllegalStateException("a do section part must be part of a do section");
            }
            this.section = section;
            this.position = position;
            this.doSectionSub = doSectionSub;
        }

        public ExecutableSection getSection() {
            return section;
        }

        public DoSectionSub getDoSectionSub() {
            return doSectionSub;
        }

        public int getPosition() {
            return position;
        }

        //intentionally not including doSectionSub
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Location location = (Location) o;
            return position == location.position &&
                section == location.section;
        }

        //intentionally not including doSectionSub
        @Override
        public int hashCode() {
            return Objects.hash(section, position);
        }

        @Override
        public String toString() {
            return "Location{" +
                "section=" + section +
                ", doSectionSub=" + doSectionSub +
                ", position=" + position +
                '}';
        }

        @Override
        public int compareTo(Location o) {
            return Integer.valueOf(position).compareTo(o.position);
        }
    }
}

