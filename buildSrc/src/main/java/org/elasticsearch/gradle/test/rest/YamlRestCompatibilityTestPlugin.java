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

import org.elasticsearch.gradle.ElasticsearchJavaPlugin;
import org.elasticsearch.gradle.test.RestIntegTestTask;
import org.elasticsearch.gradle.test.RestTestBasePlugin;
import org.elasticsearch.gradle.testclusters.TestClustersPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Apply this plugin to run the YAML based REST compatible tests. These tests are sourced from the prior version but executed against
 * the current version of the server with the headers injected to enable compatibility. This is not a general purpose plugin and should
 * only be applied to a single project.
 */
public class YamlRestCompatibilityTestPlugin implements Plugin<Project> {

    @Override
    public void apply(Project thisProject) {

        thisProject.getPluginManager().apply(ElasticsearchJavaPlugin.class);
        thisProject.getPluginManager().apply(TestClustersPlugin.class);
        thisProject.getPluginManager().apply(RestTestBasePlugin.class);
        thisProject.getPluginManager().apply(RestResourcesPlugin.class);

        Set<String> prefixes = Set.of(":modules:", ":plugins:", ":x-pack:plugin", ":rest-api-spec");
        Map<Project, SourceSet> projectsToTest = new HashMap<>();
        //find the projects within the prefixes that have yaml rest tests
        thisProject.getRootProject().getAllprojects().stream()
            .filter(p -> prefixes.stream().anyMatch(prefix -> p.getPath().startsWith(prefix)))
            .filter(p -> thisProject.equals(p) == false)
            .forEach(candidateProject -> {
                thisProject.evaluationDependsOn(candidateProject.getPath());
                candidateProject.getPluginManager().withPlugin("elasticsearch.yaml-rest-test", plugin -> {
                    SourceSetContainer sourceSets = candidateProject.getExtensions().getByType(SourceSetContainer.class);
                    SourceSet yamlTestSourceSet = sourceSets.getByName(YamlRestTestPlugin.SOURCE_SET_NAME);
                    projectsToTest.put(candidateProject, yamlTestSourceSet);
                });
            });

        projectsToTest.forEach((projectToTest, projectToTestSourceSet) -> {

            //for each project, create unique tasks and source sets
            String taskAndSourceSetName = "yamlRestCompatibilityTest" + projectToTest.getPath().replace(":", "#");

            // create source set
            SourceSetContainer sourceSets = thisProject.getExtensions().getByType(SourceSetContainer.class);
            SourceSet thisYamlTestSourceSet = sourceSets.create(taskAndSourceSetName);

            //create the test task
            RestIntegTestTask thisTestTask = thisProject.getTasks().create(taskAndSourceSetName, RestIntegTestTask.class);
            thisTestTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
            thisTestTask.setDescription("Runs the tests from " + projectToTest.getPath() + " from the prior version against a cluster of the current version");

            // configure the test task
            thisTestTask.dependsOn(projectToTest.getTasks().getByName(projectToTestSourceSet.getCompileJavaTaskName()));
            thisTestTask.setTestClassesDirs(projectToTestSourceSet.getOutput().getClassesDirs());
            thisTestTask.setClasspath(thisYamlTestSourceSet.getRuntimeClasspath()
                .plus(projectToTestSourceSet.getOutput().getClassesDirs()));
            RestTestUtil.addPluginOrModuleToTestCluster(projectToTest, thisTestTask);
            RestTestUtil.addPluginDependency(projectToTest, thisTestTask);

            //set up dependencies
            thisProject.getDependencies().add(thisYamlTestSourceSet.getImplementationConfigurationName(), thisProject.project(":test:framework"));
            thisProject.getDependencies().add(thisYamlTestSourceSet.getImplementationConfigurationName(), projectToTest);

            // copy the rest resources
            TaskProvider<Copy> thisCopyTestsTask = thisProject.getTasks().register(taskAndSourceSetName + "#copyTests", Copy.class);
            thisCopyTestsTask.configure(copy -> {
                copy.from(projectToTestSourceSet.getOutput().getResourcesDir().toPath()); //TODO: change to the real deal, yo!
                copy.into(thisYamlTestSourceSet.getOutput().getResourcesDir().toPath());
                copy.dependsOn(projectToTest.getTasks().getByName("copyYamlTestsTask"), projectToTest.getTasks().getByName("copyRestApiSpecsTask"));
            });

            thisTestTask.dependsOn(thisCopyTestsTask);
            //TODO: copy test cluster configuration from original task to new task

            //TODO: wire these into check

//            thisTestTask.doFirst(t -> {
//                thisTestTask.getClasspath().forEach(f -> System.out.println("********* -> " + f));
//            });

        });
    }
}
