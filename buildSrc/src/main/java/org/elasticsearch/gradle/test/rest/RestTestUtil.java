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
import org.elasticsearch.gradle.info.GlobalBuildInfoPlugin;
import org.elasticsearch.gradle.plugin.PluginPropertiesExtension;
import org.elasticsearch.gradle.test.RestIntegTestTask;
import org.elasticsearch.gradle.testclusters.RestTestRunnerTask;
import org.elasticsearch.gradle.testclusters.TestClustersPlugin;
import org.elasticsearch.gradle.util.GradleUtils;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Zip;

public class RestTestUtil {

    protected static void setup(Project project, String sourceSetName){
        // the test runner requires java
        project.getPluginManager().apply(ElasticsearchJavaPlugin.class);


//        project.getRootProject().getPluginManager().apply(GlobalBuildInfoPlugin.class);
//
//        project.getPluginManager().apply(JavaPlugin.class);
//        ElasticsearchJavaPlugin.configureConfigurations(project);
//        ElasticsearchJavaPlugin.configureCompile(project);
//        ElasticsearchJavaPlugin.configureInputNormalization(project);
//        ElasticsearchJavaPlugin.configureTestTasks(project);
//        ElasticsearchJavaPlugin.configureInputNormalization(project);



        // to spin up the external cluster
        project.getPluginManager().apply(TestClustersPlugin.class);
        // to copy around the yaml tests and json spec
        project.getPluginManager().apply(RestResourcesPlugin.class);

        // note - source sets are not created via org.elasticsearch.gradle.util.GradleUtils.addTestSourceSet since unlike normal tests
        // we only want the yamlTestSourceSet on the classpath by default. The yaml tests should be pure black box testing over HTTP and
        // such it should not need the main class on the class path. Also, there are some special setup steps unique to YAML tests.

        // create source set
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet restTestSourceSet = sourceSets.create(sourceSetName);

        // create task - note can not use .register due to the work in RestIntegTestTask's constructor :(
        RestIntegTestTask restTestTask = project.getTasks()
            .create(
                sourceSetName,
                RestIntegTestTask.class,
                task -> { task.dependsOn(project.getTasks().getByName("copyRestApiSpecsTask")); }
            );
        restTestTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
        restTestTask.setDescription("Runs the REST test against an external cluster");

        // setup task dependency
        project.getDependencies().add(sourceSetName + "Compile", project.project(":test:framework"));

        // setup the runner and only add this source set's output to classpath
        RestTestRunnerTask runner = (RestTestRunnerTask) project.getTasks().getByName(restTestTask.getName() + "Runner");
        runner.setTestClassesDirs(restTestSourceSet.getOutput().getClassesDirs());
        runner.setClasspath(restTestSourceSet.getRuntimeClasspath());

        // if this a module or plugin, it may have an associated zip file with it's contents, add that to the test cluster
        boolean isModule = project.getPath().startsWith(":modules:");
        Zip bundle = (Zip) project.getTasks().findByName("bundlePlugin");
        if (bundle != null) {
            restTestTask.dependsOn(bundle);
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
                        restTestTask.dependsOn(extensionBundle);
                        runner.getClusters().forEach(c -> c.module(extensionBundle.getArchiveFile()));
                    }
                });
            }
        });

        // setup IDE
        GradleUtils.setupIdeForTestSourceSet(project, restTestSourceSet);

        //TODO: follow up once : https://github.com/elastic/elasticsearch/pull/56926 is merged
        // validation of the rest specification is wired to precommit, so ensure that runs first
//        yamlTestTask.mustRunAfter(project.getTasks().getByName("precommit"));

        // wire this task into check
        project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(check -> check.dependsOn(restTestTask));

    }
}
