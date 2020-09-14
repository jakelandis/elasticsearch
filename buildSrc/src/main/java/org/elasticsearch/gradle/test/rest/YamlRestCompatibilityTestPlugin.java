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
import org.elasticsearch.gradle.testclusters.TestClustersAware;
import org.elasticsearch.gradle.testclusters.TestClustersPlugin;
import org.elasticsearch.gradle.util.GradleUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Apply this plugin to run the YAML based REST compatible tests. These tests are sourced from the prior version but executed against
 * the current version of the server with the headers injected to enable compatibility. This is not a general purpose plugin and should
 * only be applied to a single project or a similar group of projects with the same parent.
 */
public class YamlRestCompatibilityTestPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "yamlRestCompatibility";
    // The parent of a group of projects that that apply this plugin.
    // Exclude the follow path(s) from being depended up for evaluation to avoid circular evaluation dependencies,
    private static final String COMPAT_EVAL_EXCLUDE = System.getProperty("test.rest.compatibility.exclude.path", ":rest-compatibility");

    @Override
    public void apply(Project thisProject) {
        YamlRestCompatibilityExtension extension = thisProject.getExtensions().create(EXTENSION_NAME, YamlRestCompatibilityExtension.class);



        // ensure that this project evaluates after all other projects and if those projects have yaml tests add them to cithin the includes that have yaml rest tests
        Map<Project, SourceSet> projectsWithYamlTests = new HashMap<>();
        thisProject.getRootProject().getAllprojects().stream()
            .filter(p -> thisProject.equals(p) == false)
            .filter(p -> p.getPath().contains(COMPAT_EVAL_EXCLUDE) == false)
            .forEach(candidateProject -> {
                thisProject.evaluationDependsOn(candidateProject.getPath());
                candidateProject.getPluginManager().withPlugin("elasticsearch.yaml-rest-test", plugin -> {
                    SourceSetContainer sourceSets = candidateProject.getExtensions().getByType(SourceSetContainer.class);
                    SourceSet yamlTestSourceSet = sourceSets.getByName(YamlRestTestPlugin.SOURCE_SET_NAME);
                    projectsWithYamlTests.put(candidateProject, yamlTestSourceSet);
                });
            });

        //do this after evaluation to ensure that we can read from the extension
        thisProject.afterEvaluate(p -> {
            thisProject.getPluginManager().apply(ElasticsearchJavaPlugin.class);
            thisProject.getPluginManager().apply(TestClustersPlugin.class);
            thisProject.getPluginManager().apply(RestTestBasePlugin.class);
            thisProject.getPluginManager().apply(RestResourcesPlugin.class);

            List<String> includes = extension.gradleProject.getInclude().get();
            List<String> excludes = extension.gradleProject.getExclude().get();

            //create a task for each configured version
            extension.versions.forEach(version -> {
                projectsWithYamlTests.entrySet().stream()
                    .filter(entry -> includes.stream().anyMatch(include -> entry.getKey().getPath().startsWith(include)))
                    .filter(entry -> excludes.stream().noneMatch(exclude -> entry.getKey().getPath().startsWith(exclude)))
                    .forEach(entry -> {
                        Project projectToTest = entry.getKey();
                        SourceSet projectToTestSourceSet = entry.getValue();
                        //for each project, create unique tasks and source sets
                        String taskAndSourceSetName = "yamlRestCompatibilityTest#" + version.toString() + projectToTest.getPath().replace(":", "#");

                        // create source set
                        SourceSetContainer sourceSets = thisProject.getExtensions().getByType(SourceSetContainer.class);
                        SourceSet thisYamlTestSourceSet = sourceSets.create(taskAndSourceSetName);

                        //create the test task
                        RestIntegTestTask thisTestTask = thisProject.getTasks().create(taskAndSourceSetName, RestIntegTestTask.class);
                        thisTestTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
                        thisTestTask.setDescription("Runs the " + version.toString() + " tests from " + projectToTest.getPath() + " against the current version");

                        // configure the test task
                        thisTestTask.dependsOn(projectToTest.getTasks().getByName(projectToTestSourceSet.getCompileJavaTaskName()));
                        thisTestTask.setTestClassesDirs(projectToTestSourceSet.getOutput().getClassesDirs());
                        thisTestTask.setClasspath(thisYamlTestSourceSet.getRuntimeClasspath()
                            .plus(projectToTestSourceSet.getOutput().getClassesDirs())); //add the class path for this test
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


                        thisTestTask.withClusterConfig((TestClustersAware) projectToTest.getTasks().getByName(YamlRestTestPlugin.SOURCE_SET_NAME));


                        // wire this task into check
                        thisProject.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(check -> check.dependsOn(thisTestTask));

                    });
            });
        });

    }
}
