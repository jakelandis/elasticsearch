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

package org.elasticsearch.gradle;

import org.elasticsearch.gradle.info.BuildParams;
import org.elasticsearch.gradle.test.rest.CopyRestApiTask;
import org.elasticsearch.gradle.test.rest.CopyRestTestsTask;
import org.elasticsearch.gradle.test.rest.RestResourcesExtension;
import org.elasticsearch.gradle.test.rest.RestResourcesPlugin;
import org.elasticsearch.gradle.test.rest.YamlRestTestPlugin;
import org.elasticsearch.gradle.testclusters.RestTestRunnerTask;
import org.elasticsearch.gradle.util.GradleUtils;
import org.elasticsearch.gradle.util.Util;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.Zip;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RestCompatibilityPlugin implements Plugin<Project> {

    public static final String COMPATIBLE_VERSION = "v" + (VersionProperties.getElasticsearchVersion().getMajor() - 1);
    public static final String SOURCE_SET_NAME = COMPATIBLE_VERSION + "restActions";
    public static final String TEST_SOURCE_SET_NAME = COMPATIBLE_VERSION + "restActionsTest";

    @Override
    public void apply(Project project) {
        if(BuildParams.isInternal() == false){
            //TODO: support this for plugin development
            throw new IllegalStateException("Rest Compatibility is not supported for plugin development");
        }
        project.getPluginManager().apply(ElasticsearchJavaPlugin.class);

        // create rest-compatibility source set
        final SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet restCompatSourceSet = sourceSets.create(SOURCE_SET_NAME);

        Configuration restCompatCompileConfig = project.getConfigurations().getByName(restCompatSourceSet.getCompileClasspathConfigurationName());
        Configuration restCompatRuntimeConfig = project.getConfigurations().getByName(restCompatSourceSet.getRuntimeClasspathConfigurationName());
        restCompatSourceSet.setCompileClasspath(restCompatCompileConfig);
        restCompatSourceSet.setRuntimeClasspath(project.getObjects().fileCollection().from(restCompatSourceSet.getOutput(), restCompatRuntimeConfig));


        //compat code depend on main code
        GradleUtils.extendSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME, SOURCE_SET_NAME);


        GradleUtils.addTestSourceSet(project, TEST_SOURCE_SET_NAME);
        //tests need access to compat and main code
        GradleUtils.extendSourceSet(project, SOURCE_SET_NAME, TEST_SOURCE_SET_NAME);
        SourceSet restCompatTestSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        Configuration restCompatTestCompileConfig = project.getConfigurations().getByName(restCompatTestSourceSet.getCompileClasspathConfigurationName());
        Dependency testFrameworkDependency = project.getDependencies().project(Map.of("path", ":test:framework"));
        project.getDependencies().add(restCompatTestCompileConfig.getName(), testFrameworkDependency);

        //include output in jar
        Jar jarTask = (Jar) project.getTasks().getByName("jar");
        jarTask.from(restCompatSourceSet.getOutput());
        jarTask.dependsOn(restCompatCompileConfig);

        //required so that the checkoutDir extension is populated
        project.evaluationDependsOn(":distribution:bwc");

        GradleUtils.setupIdeForNonTestSourceSet(project, restCompatSourceSet);
        GradleUtils.setupIdeForTestSourceSet(project, restCompatTestSourceSet);
    }
}
