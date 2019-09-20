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
package org.elasticsearch.client.ilm;

import org.elasticsearch.common.unit.TimeValue;

import java.util.Map;
import java.util.Objects;

/**
 * Represents set of {@link LifecycleAction}s which should be executed at a
 * particular point in the lifecycle of an index.
 */
public class Phase {


    private final String name;
    private final Map<String, LifecycleAction> actions;
    private final TimeValue minimumAge;

    /**
     * @param name
     *            the name of this {@link Phase}.
     * @param minimumAge
     *            the age of the index when the index should move to this
     *            {@link Phase}.
     * @param actions
     *            a {@link Map} of the {@link LifecycleAction}s to run when
     *            during this {@link Phase}. The keys in this map are the associated
     *            action names.
     */
    public Phase(String name, TimeValue minimumAge, Map<String, LifecycleAction> actions) {
        this.name = name;
        if (minimumAge == null) {
            this.minimumAge = TimeValue.ZERO;
        } else {
            this.minimumAge = minimumAge;
        }
        this.actions = actions;
    }

    /**
     * @return the age of the index when the index should move to this
     *         {@link Phase}.
     */
    public TimeValue getMinimumAge() {
        return minimumAge;
    }

    /**
     * @return the name of this {@link Phase}
     */
    public String getName() {
        return name;
    }

    /**
     * @return a {@link Map} of the {@link LifecycleAction}s to run when during
     *         this {@link Phase}.
     */
    public Map<String, LifecycleAction> getActions() {
        return actions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, minimumAge, actions);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        Phase other = (Phase) obj;
        return Objects.equals(name, other.name) &&
            Objects.equals(minimumAge, other.minimumAge) &&
            Objects.equals(actions, other.actions);
    }

    @Override
    public String toString() {
        return "Phase{" +
            "name='" + name + '\'' +
            ", actions=" + actions +
            ", minimumAge=" + minimumAge +
            '}';
    }
}
