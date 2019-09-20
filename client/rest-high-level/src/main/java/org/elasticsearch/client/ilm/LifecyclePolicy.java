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

import org.elasticsearch.common.util.set.Sets;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the lifecycle of an index from creation to deletion. A
 * {@link LifecyclePolicy} is made up of a set of {@link Phase}s which it will
 * move through.
 */
public class LifecyclePolicy {

    private static Map<String, Set<String>> ALLOWED_ACTIONS = new HashMap<>();

    static {

        ALLOWED_ACTIONS.put("hot", Sets.newHashSet(UnfollowAction.NAME, SetPriorityAction.NAME, RolloverAction.NAME));
        ALLOWED_ACTIONS.put("warm", Sets.newHashSet(UnfollowAction.NAME, SetPriorityAction.NAME, AllocateAction.NAME, ForceMergeAction.NAME,
            ReadOnlyAction.NAME, ShrinkAction.NAME));
        ALLOWED_ACTIONS.put("cold", Sets.newHashSet(UnfollowAction.NAME, SetPriorityAction.NAME, AllocateAction.NAME, FreezeAction.NAME));
        ALLOWED_ACTIONS.put("delete", Sets.newHashSet(DeleteAction.NAME));
    }

    private final String name;
    private final Map<String, Phase> phases;

    /**
     * @param name
     *            the name of this {@link LifecyclePolicy}
     * @param phases
     *            a {@link Map} of {@link Phase}s which make up this
     *            {@link LifecyclePolicy}.
     */
    public LifecyclePolicy(String name, Map<String, Phase> phases) {
       phases.values().forEach(phase -> {
            if (ALLOWED_ACTIONS.containsKey(phase.getName()) == false) {
                throw new IllegalArgumentException("Lifecycle does not support phase [" + phase.getName() + "]");
            }
            phase.getActions().forEach((actionName, action) -> {
                if (ALLOWED_ACTIONS.get(phase.getName()).contains(actionName) == false) {
                    throw new IllegalArgumentException("invalid action [" + actionName + "] " +
                        "defined in phase [" + phase.getName() +"]");
                }
            });
        });
        this.name = name;
        this.phases = phases;
    }

    /**
     * @return the name of this {@link LifecyclePolicy}
     */
    public String getName() {
        return name;
    }

    /**
     * @return the {@link Phase}s for this {@link LifecyclePolicy} in the order
     *         in which they will be executed.
     */
    public Map<String, Phase> getPhases() {
        return phases;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, phases);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        LifecyclePolicy other = (LifecyclePolicy) obj;
        return Objects.equals(name, other.name) &&
            Objects.equals(phases, other.phases);
    }

    @Override
    public String toString() {
        return "LifecyclePolicy{" +
            "name='" + name + '\'' +
            ", phases=" + phases +
            '}';
    }
}
