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

import org.elasticsearch.gradle.test.rest.RestTestUtil;
import org.elasticsearch.gradle.testclusters.ElasticsearchCluster;
import org.elasticsearch.gradle.testclusters.RestTestRunnerTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;

public class RestCompatTestsPlugin implements Plugin<Project> {
    private static final String TASK_NAME = "restCompatTests";
    private ElasticsearchCluster useTestCluster;
    @Override
    public void apply(Project project) {

        project.getTasks().create(TASK_NAME, RestTestRunnerTask.class, task -> {
           // task.dependsOn(project.getRootProject().getTasks().named("prepRestCompatTests"));
            RestTestRunnerTask runner = RestTestUtil.setupRunner(project, TASK_NAME, useTestCluster);
            task.dependsOn(runner);
            task.mustRunAfter(project.getTasks().named("precommit"));


//            RestTestRunnerTask runner = (RestTestRunnerTask) project.getTasks().getByName("restCompatTestsRunner");
//            SystemPropertyCommandLineArgumentProvider nonInputProperties = (SystemPropertyCommandLineArgumentProvider) runner.getExtensions().getByName("nonInputProperties");
//            nonInputProperties.systemProperty("tests.rest.compat", Boolean.TRUE.toString());
//
//            FactoryNamedDomainObjectContainer testClusters = (FactoryNamedDomainObjectContainer) project.getExtensions().getByName("testClusters");
//            System.out.println("********testclusters*** " + testClusters.getNames().stream().collect(Collectors.joining(",")));
//            ElasticsearchCluster testCluster = (ElasticsearchCluster) testClusters.findByName("integTest");
//            System.out.println("***********"+ testCluster);

//System.out.println("******nonInputProperties******* " + nonInputProperties);
//System.out.println("******testClusters******* " + project.getExtensions().getByName("testClusters").getClass());


            //runner.useCluster();
            // runner.systemProperty()
            ;//.nonInputProperties.systemProperty 'tests.rest.compat', "true"
        });
       // project.getTasks().named("check")

    }

    @Input
    public ElasticsearchCluster getUseTestCluster(){
        return useTestCluster;
    }

    public void setUseTestCluster(ElasticsearchCluster cluster){
        this.useTestCluster = cluster;
    }

}
