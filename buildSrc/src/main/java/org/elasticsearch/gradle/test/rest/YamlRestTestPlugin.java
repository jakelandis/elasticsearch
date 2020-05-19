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
import org.elasticsearch.gradle.plugin.PluginPropertiesExtension;
import org.elasticsearch.gradle.test.RestIntegTestTask;
import org.elasticsearch.gradle.testclusters.RestTestRunnerTask;
import org.elasticsearch.gradle.testclusters.TestClustersPlugin;
import org.elasticsearch.gradle.util.GradleUtils;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Zip;

/**
 * Apply this plugin to run the YAML based REST tests. This will adda
 */
public class YamlRestTestPlugin implements Plugin<Project> {

    public static final String SOURCE_SET_NAME = "yamlRestTest";

    @Override
    public void apply(Project project) {


        project.getPluginManager().apply(ElasticsearchJavaPlugin.class);
        // to spin up the external cluster
        project.getPluginManager().apply(TestClustersPlugin.class);
        // to copy around the yaml tests and json spec
        project.getPluginManager().apply(RestResourcesPlugin.class);

        // note - source sets are not created via org.elasticsearch.gradle.util.GradleUtils.addTestSourceSet since unlike normal tests
        // we only want the yamlTestSourceSet on the classpath by default. The yaml tests should be pure black box testing over HTTP and
        // such it should not need the main class on the class path. Also, there are some special setup steps unique to YAML tests.

        // create source set
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet yamlTestSourceSet = sourceSets.create(SOURCE_SET_NAME);

        // create task - note can not use .register due to the work in RestIntegTestTask's constructor :(
        RestIntegTestTask yamlTestTask = project.getTasks()
            .create(
                SOURCE_SET_NAME,
                RestIntegTestTask.class,
                task -> { task.dependsOn(project.getTasks().getByName("copyRestApiSpecsTask")); }
            );
        yamlTestTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
        yamlTestTask.setDescription("Runs the YAML based REST tests against an external cluster");

        // setup task dependency
        project.getDependencies().add(SOURCE_SET_NAME + "Compile", project.project(":test:framework"));

        // setup the runner
        RestTestRunnerTask runner = (RestTestRunnerTask) project.getTasks().getByName(yamlTestTask.getName() + "Runner");
        runner.setTestClassesDirs(yamlTestSourceSet.getOutput().getClassesDirs());
        runner.setClasspath(yamlTestSourceSet.getRuntimeClasspath());

        // if this a module or plugin, it may have an associated zip file with it's contents, add that to the test cluster
        boolean isModule = project.getPath().startsWith(":modules:");
        Zip bundle = (Zip) project.getTasks().findByName("bundlePlugin");
        if (bundle != null) {
            yamlTestTask.dependsOn(bundle);
            if (isModule) {
                runner.getClusters().forEach(c -> c.module(bundle.getArchiveFile()));
            } else {
                runner.getClusters().forEach(c -> c.plugin(project.getObjects().fileProperty().value(bundle.getArchiveFile())));
            }
        }

        // es-plugins may declare dependencies on additional modules, add those to the test cluster too.
        project.afterEvaluate(p -> {
            PluginPropertiesExtension pluginPropertiesExtension = project.getExtensions().findByType(PluginPropertiesExtension.class);
            if (pluginPropertiesExtension != null) { // not all projects are defined as plugins
                pluginPropertiesExtension.getExtendedPlugins().forEach(pluginName -> {
                    Project extensionProject = project.getProject().findProject(":modules:" + pluginName);
                    if (extensionProject != null) { // extension plugin may be defined, but not required to be a module
                        Zip extensionBundle = (Zip) extensionProject.getTasks().getByName("bundlePlugin");
                        yamlTestTask.dependsOn(extensionBundle);
                        runner.getClusters().forEach(c -> c.module(extensionBundle.getArchiveFile()));
                    }
                });
            }
        });

        // setup IDE
        GradleUtils.setupIdeForTestSourceSet(project, yamlTestSourceSet);

        // run test tasks first if they exist since they are presumably faster and less resources intensive
        Task testTask = project.getTasks().findByName("test");
        if (testTask != null) {
            yamlTestTask.mustRunAfter(testTask);
        }

        // validation of the rest specification is wired to precommit, so ensure that runs first
//        yamlTestTask.mustRunAfter(project.getTasks().getByName("precommit"));

        // wire this task into check
        project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(check -> check.dependsOn(yamlTestTask));
    }
}
