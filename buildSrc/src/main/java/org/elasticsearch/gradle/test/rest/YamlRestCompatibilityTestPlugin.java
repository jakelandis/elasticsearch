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
import org.elasticsearch.gradle.Version;
import org.elasticsearch.gradle.info.BuildParams;
import org.elasticsearch.gradle.test.RestIntegTestTask;
import org.elasticsearch.gradle.test.RestTestBasePlugin;
import org.elasticsearch.gradle.testclusters.ElasticsearchCluster;
import org.elasticsearch.gradle.testclusters.ElasticsearchNode;
import org.elasticsearch.gradle.testclusters.TestClustersPlugin;
import org.elasticsearch.gradle.testclusters.TestDistribution;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;


/**
 * Apply this plugin to run the YAML based REST compatible tests. These tests are sourced from the prior version but executed against
 * the current version of the server with the headers injected to enable compatibility. This is not a general purpose plugin and should
 * only be applied to a single project or a similar group of projects with the same parent project.
 */
public class YamlRestCompatibilityTestPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "yamlRestCompatibility";
    // Exclude the follow paths with the following from being depended up for evaluation to avoid circular evaluation dependencies
    private static final String COMPAT_TESTS_PARENT = ":rest-compatibility:qa";

    private static final Path RELATIVE_API_PATH = Path.of("rest-api-spec/api");
    private static final Path RELATIVE_TEST_PATH = Path.of("rest-api-spec/test");


    public void apply(Project thisProject) {
        if (BuildParams.isInternal() == false) {
            throw new IllegalStateException("Compatibility testing is not supported externally");
        }
        YamlRestCompatibilityExtension extension = thisProject.getExtensions().create(EXTENSION_NAME, YamlRestCompatibilityExtension.class);

        // ensure that this project evaluates after all other projects and take note of any project that has yaml tests
        Map<Project, SourceSet> projectsWithYamlTests = new HashMap<>();
        thisProject.getRootProject().getAllprojects().stream()
            .filter(p -> thisProject.equals(p) == false)
            .filter(p -> p.getPath().contains(COMPAT_TESTS_PARENT) == false)
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
            if (extension.enabled.get() == false) {
                thisProject.getPluginManager().apply(ElasticsearchJavaPlugin.class);
                return;
            }
            thisProject.getPluginManager().apply(ElasticsearchJavaPlugin.class);
            thisProject.getPluginManager().apply(TestClustersPlugin.class);
            thisProject.getPluginManager().apply(RestTestBasePlugin.class);
            thisProject.getPluginManager().apply(RestResourcesPlugin.class);

            //read the extension
            List<String> include = extension.gradleProject.getInclude().get();
            List<String> exclude = extension.gradleProject.getExclude().get();
            List<String> includeOnly = extension.gradleProject.getIncludeOnly().get();
            List<String> excludeOnly = extension.gradleProject.getExcludeOnly().get();


            Predicate<Map.Entry<Project, SourceSet>> gradleInclude = entry ->
                includeOnly.isEmpty() == false
                    ? includeOnly.stream().anyMatch(i -> entry.getKey().getPath().equalsIgnoreCase(i))
                    : include.stream().map(s -> ":x-pack:plugin".equals(s) ? s + ":" : s) //add the trailing colon to disambiguate
                    .anyMatch(i -> entry.getKey().getPath().startsWith(i));

            Predicate<Map.Entry<Project, SourceSet>> gradleExclude =
                excludeOnly.isEmpty() == false
                    ? entry -> exclude.stream().noneMatch(e -> entry.getKey().getPath().equalsIgnoreCase(e))
                    : entry -> exclude.stream().map(s -> ":x-pack:plugin".equals(s) ? s + ":" : s) //add the trailing colon to disambiguate
                    .noneMatch(e -> entry.getKey().getPath().startsWith(e));

            //create a task for each version
            for (Version version : extension.versions.get()) {
                // TODO: support arbitrary versions
                assert BuildParams.getBwcVersions().getUnreleased().get(1).equals(version) : "bwc minor is the only supported version";
                projectsWithYamlTests.entrySet().stream().filter(gradleInclude).filter(gradleExclude).forEach(entry -> {

                    Project projectToTest = entry.getKey();
                    SourceSet projectToTestSourceSet = entry.getValue();
                    //for each project, create unique tasks and source sets
                    String taskAndSourceSetName = "yamlRestCompatibilityTest#" + version.toString() + projectToTest.getPath().replace(":", "#");

                    // create source set
                    SourceSetContainer sourceSets = thisProject.getExtensions().getByType(SourceSetContainer.class);
                    SourceSet thisYamlTestSourceSet = sourceSets.create(taskAndSourceSetName);

                    // copy the rest resources
                    TaskProvider<Copy> copyTaskProvider = thisProject.getTasks().register(taskAndSourceSetName + "#copyTests", Copy.class);
                    copyTaskProvider.configure(copy -> {
                        copy.from(projectToTestSourceSet.getOutput().getResourcesDir().toPath()); //TODO: change to the real deal, yo!
                        copy.into(thisYamlTestSourceSet.getOutput().getResourcesDir().toPath());
                        copy.dependsOn(projectToTest.getTasks().getByName("copyYamlTestsTask"), projectToTest.getTasks().getByName("copyRestApiSpecsTask"));
                    });

                    //create the test cluster, this ensures that the distribution is wired in correctly
                    RestTestUtil.createTestCluster(thisProject, thisYamlTestSourceSet);

                    //create the test task
                    Provider<RestIntegTestTask> testTaskProvider = thisProject.getTasks()
                        .register(taskAndSourceSetName, RestIntegTestTask.class, testTask -> {
                            testTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
                            testTask.setDescription("Runs the " + version.toString() + " tests from " + projectToTest.getPath() + " against the current version");
                            // clone the test cluster config
                            ElasticsearchCluster copyCluster = ((NamedDomainObjectContainer<ElasticsearchCluster>)
                                (projectToTest.getExtensions().getByName(TestClustersPlugin.EXTENSION_NAME)))
                                .getByName("yamlRestTest");
                            ElasticsearchCluster keepCluster = testTask.getClusters().iterator().next();
                            if (copyCluster.getNumberOfNodes() != keepCluster.getNumberOfNodes()) {
                                keepCluster.setNumberOfNodes(copyCluster.getNumberOfNodes());
                            }
                            List<ElasticsearchNode> nodes = new ArrayList<>(copyCluster.getNumberOfNodes());
                            Iterator<ElasticsearchNode> keepIterator = keepCluster.getNodes().iterator();
                            Iterator<ElasticsearchNode> copyIterator = copyCluster.getNodes().iterator();
                            while (keepIterator.hasNext() && copyIterator.hasNext()) {
                                nodes.add(new ElasticsearchNode(keepIterator.next(), copyIterator.next()));
                            }
                            //use the default distribution
                            nodes.forEach(node -> {
                                node.setTestDistribution(TestDistribution.DEFAULT);
                            });
                            // swap out the nodes with the cloned nodes
                            keepCluster.getNodes().clear();
                            keepCluster.getNodes().addAll(nodes);
                            // configure the test task
                            testTask.dependsOn(projectToTest.getTasks().getByName(projectToTestSourceSet.getCompileJavaTaskName()));
                            testTask.setTestClassesDirs(projectToTestSourceSet.getOutput().getClassesDirs());
                            testTask.setClasspath(thisYamlTestSourceSet.getRuntimeClasspath()
                                .plus(projectToTestSourceSet.getOutput().getClassesDirs())); //only add the java classes to the classpath

                            RestTestUtil.addPluginOrModuleToTestCluster(projectToTest, testTask);
                            RestTestUtil.addPluginDependency(projectToTest, testTask);
                            //ensure tests are copied
                            testTask.dependsOn(copyTaskProvider);
                        });

                    // wire this task into check
                    thisProject.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(check -> check.dependsOn(testTaskProvider));

                    //set up dependencies
                    thisProject.getDependencies().add(thisYamlTestSourceSet.getImplementationConfigurationName(), thisProject.project(":test:framework"));
                    thisProject.getDependencies().add(thisYamlTestSourceSet.getImplementationConfigurationName(), projectToTest);
                });
            }
        });
    }


    //                        start bwc
//                        final Path checkoutDir = thisProject.findProject(":distribution:bwc:minor").getBuildDir().toPath()
//                            .resolve("bwc").resolve("checkout-" + version.getMajor() + ".x");
//
//                        // copy the APIs
//                        TaskProvider<Copy> thisCopyApiTask = thisProject.getTasks().register(taskAndSourceSetName + "#copyAPIs", Copy.class);
//                        thisCopyApiTask.configure(copy -> {
//                            // copy core apis
//                            copy.from(checkoutDir.resolve("rest-api-spec/src/main/resources").resolve(RELATIVE_API_PATH));
//
//                            //copy xpack api's
//                            if (projectToTest.getPath().startsWith(":x-pack")) {
//                                copy.from(checkoutDir.resolve("x-pack/plugin/src/test/resources").resolve(RELATIVE_API_PATH));
//                            }
//
//                            // copy any module or plugin api's
//                            if (projectToTest.getPath().startsWith(":modules")
//                                || projectToTest.getPath().startsWith(":plugins")
//                                || projectToTest.getPath().startsWith(":x-pack:plugin:")) { // trailing colon intentional to disambiguate
//                                copy.from(checkoutDir.resolve(projectToTest.getPath().replaceFirst(":", "").replace(":", File.separator))
//                                    .resolve("src/yamlRestTest/resources").resolve(RELATIVE_API_PATH));
//                            }
//                            copy.into(thisYamlTestSourceSet.getOutput().getResourcesDir().toPath().resolve(RELATIVE_API_PATH));
//                            copy.dependsOn(":distribution:bwc:minor:checkoutBwcBranch");
//                        });
//
//                        // copy the tests as needed
//                        TaskProvider<Copy> thisCopyTestTask = thisProject.getTasks().register(taskAndSourceSetName + "#copyTests", Copy.class);
//                        thisCopyTestTask.configure(copy -> {
//                            //copy core tests
//                            if (projectToTest.getPath().equalsIgnoreCase(":rest-api-spec")) {
//                                copy.from(checkoutDir.resolve("rest-api-spec/src/main/resources").resolve(RELATIVE_TEST_PATH));
//                            }
//                            // copy module or plugin tests
//                            if (projectToTest.getPath().startsWith(":modules")
//                                || projectToTest.getPath().startsWith(":plugins")
//                                || projectToTest.getPath().startsWith(":x-pack:plugin:")) { // trailing colon intentional to disambiguate
//                                copy.from(checkoutDir
//                                    .resolve(projectToTest.getPath().replaceFirst(":", "").replace(":", File.separator))
//                                    .resolve("src/yamlRestTest/resources").resolve(RELATIVE_TEST_PATH));
//                            }
//                            //copy xpack tests
//                            if (projectToTest.getPath().equalsIgnoreCase(":x-pack:plugin")) {
//                                copy.from(checkoutDir.resolve("x-pack/plugin/src/test/resources").resolve(RELATIVE_TEST_PATH));
//                            }
//                            copy.into(thisYamlTestSourceSet.getOutput().getResourcesDir().toPath().resolve(RELATIVE_TEST_PATH));
//                            copy.dependsOn(thisCopyApiTask);
//                        });
//
//                        thisTestTask.dependsOn(thisCopyTestTask);
//                        //TODO: make the cluster clone or not clone configuraable.
//end bwc


}
