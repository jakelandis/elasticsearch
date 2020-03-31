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

import org.elasticsearch.gradle.SystemPropertyCommandLineArgumentProvider;
import org.elasticsearch.gradle.testclusters.ElasticsearchCluster;
import org.elasticsearch.gradle.testclusters.RestTestRunnerTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

import java.util.function.Supplier;

/**
 * Common/shared setup for the Rest integration tests
 */
public class RestTestUtil {

    private RestTestUtil() {
    }

    public static RestTestRunnerTask setupRunner(Project project, String baseName, ElasticsearchCluster testCluster) {
        RestTestRunnerTask runner = project.getTasks().create(baseName + "Runner", RestTestRunnerTask.class);

        SystemPropertyCommandLineArgumentProvider runnerNonInputProperties =
            (SystemPropertyCommandLineArgumentProvider) runner.getExtensions().getByName("nonInputProperties");
        NamedDomainObjectContainer<ElasticsearchCluster> testClusters =
            (NamedDomainObjectContainer<ElasticsearchCluster> ) project.getExtensions().getByName("testClusters");

        ElasticsearchCluster cluster = testCluster == null ? testClusters.create(baseName)     : testCluster;

        runner.useCluster(cluster);

        runner.include("**/*IT.class");
        runner.systemProperty("tests.rest.load_packaged", Boolean.FALSE.toString());

        if (System.getProperty("tests.rest.cluster") == null) {
            if (System.getProperty("tests.cluster") != null || System.getProperty("tests.clustername") != null) {
                throw new IllegalArgumentException("tests.rest.cluster, tests.cluster, and tests.clustername must all be null or non-null");
            }
            runnerNonInputProperties.systemProperty("tests.rest.cluster",
                (Supplier<String>) () -> String.join(",", cluster.getAllHttpSocketURI()));
            runnerNonInputProperties.systemProperty("tests.cluster",
                (Supplier<String>) () -> String.join(",", cluster.getAllTransportPortURI()));
            runnerNonInputProperties.systemProperty("tests.clustername",
                (Supplier<String>) cluster::getName);

        } else {
            if (System.getProperty("tests.cluster") == null || System.getProperty("tests.clustername") == null) {
                throw new IllegalArgumentException("tests.rest.cluster, tests.cluster, and tests.clustername must all be null or non-null");
            }
            // an external cluster was specified and all responsibility for cluster configuration is taken by the user
            runner.systemProperty("tests.rest.cluster", System.getProperty("tests.rest.cluster"));
            runner.systemProperty("test.cluster", System.getProperty("tests.cluster"));
            runner.systemProperty("test.clustername", System.getProperty("tests.clustername"));
        }
        return runner;
    }
}
