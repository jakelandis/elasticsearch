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
package org.elasticsearch.gradle.testclusters;

import org.elasticsearch.gradle.LazyPropertyMap;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFile;
import org.gradle.api.tasks.Nested;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;

public interface TestClustersAware extends Task {

    @Nested
    Collection<ElasticsearchCluster> getClusters();

    default void useCluster(ElasticsearchCluster cluster) {
        if (cluster.getPath().equals(getProject().getPath()) == false) {
            throw new TestClustersException("Task " + getPath() + " can't use test cluster from" + " another project " + cluster);
        }

        cluster.getNodes().stream().flatMap(node -> node.getDistributions().stream()).forEach(distro -> dependsOn(distro.getExtracted()));
        cluster.getNodes().forEach(node -> dependsOn((Callable<Collection<Configuration>>) node::getPluginAndModuleConfigurations));
        getClusters().add(cluster);
    }

    default void withClusterConfig(TestClustersAware task) {
        this.getProject().evaluationDependsOn(task.getProject().getPath());
        if (task.getClusters().size() != 1 || this.getClusters().size() != 1) {
            throw new TestClustersException("Task " + getPath() + " can't copy configuration from " + task.getPath()
                + " both tasks must have only 1 test cluster");
        }


        ElasticsearchCluster thatCluster = task.getClusters().iterator().next();
        ElasticsearchCluster thisCluster = this.getClusters().iterator().next();


        if (thisCluster.getNumberOfNodes() != thatCluster.getNumberOfNodes()) {
            thisCluster.setNumberOfNodes(thatCluster.getNumberOfNodes());
        }
        Iterator<ElasticsearchNode> thatNodeIterator = thatCluster.getNodes().iterator();
        Iterator<ElasticsearchNode> thisNodeIterator = thisCluster.getNodes().iterator();
        while (thatNodeIterator.hasNext()) {
            assert thisNodeIterator.hasNext(); //should never happen
            ElasticsearchNode thisNode = thisNodeIterator.next();
            ElasticsearchNode thatNode = thatNodeIterator.next();
            //copy the lazy maps
            thisNode.environment(thatNode.getEnvironmentRaw());
            thisNode.settings(thatNode.getSettingsRaw());

            //module and plugins
            thatNode.getModulesRaw().forEach(module -> thisNode.module(module));
            thatNode.getPluginsRaw().forEach(plugin -> thisNode.plugin(plugin));

        }








    }

    default void beforeStart() {
    }

}
