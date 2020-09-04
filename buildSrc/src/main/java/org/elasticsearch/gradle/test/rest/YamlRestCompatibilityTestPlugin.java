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

import java.nio.file.Path;
import java.nio.file.Paths;
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

            SourceSetContainer sourceSets = thisProject.getExtensions().getByType(SourceSetContainer.class);
            SourceSet thisYamlTestSourceSet = sourceSets.create(taskAndSourceSetName);
            RestIntegTestTask thisTestTask = thisProject.getTasks().create(taskAndSourceSetName, RestIntegTestTask.class);
            thisTestTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
            thisTestTask.setDescription("Runs the tests from " + projectToTest.getPath() + " from the prior version against a cluster of the current version");

            // thisTestTask.systemProperty("rest.test.prefix", "/" + taskAndSourceSetName);

            thisTestTask.dependsOn(projectToTest.getTasks().getByName(projectToTestSourceSet.getCompileJavaTaskName()));

            thisTestTask.setTestClassesDirs(projectToTestSourceSet.getOutput().getClassesDirs());


            thisProject.getDependencies().add(thisYamlTestSourceSet.getImplementationConfigurationName(), thisProject.project(":test:framework"));

            thisTestTask.setClasspath(thisYamlTestSourceSet.getRuntimeClasspath().plus(projectToTestSourceSet.getRuntimeClasspath()
                    .filter(f -> f.getAbsoluteFile().toPath().endsWith(Path.of("resources", YamlRestTestPlugin.SOURCE_SET_NAME)) == false)));

            // setup the copy for the rest resources

            TaskProvider<Copy> thisCopyTestsTask = thisProject.getTasks().register(taskAndSourceSetName + "#copyTests", Copy.class);
            thisCopyTestsTask.configure(copy -> {
                copy.from(projectToTestSourceSet.getOutput().getResourcesDir().toPath()); //TODO: change to the real deal, yo!
                copy.into(thisYamlTestSourceSet.getOutput().getResourcesDir().toPath());
                copy.dependsOn(projectToTest.getTasks().getByName("copyYamlTestsTask"), projectToTest.getTasks().getByName("copyRestApiSpecsTask"));
            });

            thisTestTask.dependsOn(thisCopyTestsTask);


            thisTestTask.doFirst(t -> {
                thisTestTask.getClasspath().forEach(f -> System.out.println("********* -> " + f));
            });

        });


//
//        // create source set
//        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
//        SourceSet thisYamlTestSourceSet = sourceSets.create(SOURCE_SET_NAME);
//
//        // setup the yamlRestTest task
//        RestIntegTestTask yamlRestTestTask = setupTask(project, SOURCE_SET_NAME);
//
//        // setup the runner task
//        setupRunnerTask(project, yamlRestTestTask, thisYamlTestSourceSet);
//
//        // setup the dependencies
//        setupDependencies(project, thisYamlTestSourceSet);
//
//        // setup the copy for the rest resources
//        project.getTasks().withType(CopyRestApiTask.class, copyRestApiTask -> {
//            copyRestApiTask.sourceSetName = SOURCE_SET_NAME;
//            project.getTasks().named(thisYamlTestSourceSet.getProcessResourcesTaskName()).configure(t -> t.dependsOn(copyRestApiTask));
//        });
//        project.getTasks().withType(CopyRestTestsTask.class, copyRestTestTask -> { copyRestTestTask.sourceSetName = SOURCE_SET_NAME; });
//
//        // setup IDE
//        GradleUtils.setupIdeForTestSourceSet(project, thisYamlTestSourceSet);
//
//        // wire this task into check
//        project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(check -> check.dependsOn(yamlRestTestTask));
    }
}
