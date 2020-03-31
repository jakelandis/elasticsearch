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
package org.elasticsearch.gradle.test


import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.testing.Test
import org.elasticsearch.gradle.test.rest.RestTestUtil

/**
 * A wrapper task around setting up a cluster and running rest tests.
 */
class RestIntegTestTask extends DefaultTask {

    protected Test runner

    RestIntegTestTask() {
        runner = RestTestUtil.setupRunner(project, name, null);
        super.dependsOn(runner)
        // this must run after all projects have been configured, so we know any project
        // references can be accessed as a fully configured
        project.gradle.projectsEvaluated {
            if (enabled == false) {
                runner.enabled = false
                return // no need to add cluster formation tasks if the task won't run!
            }
        }
    }

    @Override
    public Task dependsOn(Object... dependencies) {
        runner.dependsOn(dependencies)
        for (Object dependency : dependencies) {
            if (dependency instanceof Fixture) {
                runner.finalizedBy(((Fixture) dependency).getStopTask())
            }
        }
        return this
    }

    @Override
    public void setDependsOn(Iterable<?> dependencies) {
        runner.setDependsOn(dependencies)
        for (Object dependency : dependencies) {
            if (dependency instanceof Fixture) {
                runner.finalizedBy(((Fixture) dependency).getStopTask())
            }
        }
    }

    public void runner(Closure configure) {
        project.tasks.getByName("${name}Runner").configure(configure)
    }

}
