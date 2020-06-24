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
import org.elasticsearch.gradle.util.GradleUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.idea.model.IdeaModel;

import java.util.Map;

public class RestCompatibilityPlugin implements Plugin<Project> {

    private final String PREFIX = "v" + (VersionProperties.getElasticsearchVersion().getMajor() - 1);
    private final String SOURCE_SET_NAME = PREFIX + "restCompatibility";
    private final String TEST_SOURCE_SET_NAME = PREFIX + "restCompatibilityTest";

    @Override
    public void apply(Project project) {
     //   project.getPluginManager().apply(ElasticsearchJavaPlugin.class);

        // create rest-compatibility source set
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet restCompatSourceSet = sourceSets.create(SOURCE_SET_NAME);

        Configuration restCompatCompileConfig = project.getConfigurations().getByName(restCompatSourceSet.getCompileClasspathConfigurationName());
        Configuration restCompatRuntimeConfig = project.getConfigurations().getByName(restCompatSourceSet.getRuntimeClasspathConfigurationName());
        restCompatSourceSet.setCompileClasspath(restCompatCompileConfig);
        restCompatSourceSet.setRuntimeClasspath(project.getObjects().fileCollection().from(restCompatSourceSet.getOutput(), restCompatRuntimeConfig));

        GradleUtils.extendSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME, SOURCE_SET_NAME);

        Dependency restCompatDependency = project.getDependencies().project(Map.of("path", ":modules:rest-compatibility")); //TOOD: protect with BuildParams.isInternal
        project.getDependencies().add(restCompatCompileConfig.getName(), restCompatDependency);


//        // create the rest-compatibility-test source set
        GradleUtils.addTestSourceSet(project, TEST_SOURCE_SET_NAME);
        GradleUtils.extendSourceSet(project, SOURCE_SET_NAME, TEST_SOURCE_SET_NAME);
        SourceSet restCompatTestSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        Configuration restCompatTestCompileConfig  = project.getConfigurations().getByName(restCompatTestSourceSet.getCompileClasspathConfigurationName());
        if (BuildParams.isInternal()) {
            Dependency testFrameworkDependency = project.getDependencies().project(Map.of("path", ":test:framework"));
            Dependency restCompatTestDependency = project.getDependencies().project(Map.of("path", ":modules:rest-compatibility",  "configuration", "testArtifacts"));
            project.getDependencies().add(restCompatTestCompileConfig.getName(), testFrameworkDependency);
            project.getDependencies().add(restCompatTestCompileConfig.getName(), restCompatDependency);
            project.getDependencies().add(restCompatTestCompileConfig.getName(), restCompatTestDependency);
        } else {

            project.getDependencies().add(restCompatTestCompileConfig.getName(),
                "org.elasticsearch.test:framework:" + VersionProperties.getElasticsearch());
            //TODO: ensure modules:rest-compatibility
            //TODO: ensure modules:rest-compatibility , testArtifacts configuration

        }


//
//
//        //add the restCompatibility source set to the to the classpaths, don't override the existing classpath
//        SourceSet restCompatTestSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
//        Configuration restCompatTestCompileConfig  = project.getConfigurations().getByName(restCompatTestSourceSet.getCompileClasspathConfigurationName());
//        Configuration restCompatTestRuntimeConfig = project.getConfigurations().getByName(restCompatTestSourceSet.getRuntimeClasspathConfigurationName());
//        restCompatTestSourceSet.setCompileClasspath(project.getObjects().fileCollection().from(restCompatSourceSet.getOutput(), restCompatTestCompileConfig));
//        restCompatTestSourceSet.setRuntimeClasspath(project.getObjects().fileCollection().from(restCompatSourceSet.getOutput(), restCompatTestRuntimeConfig));
////add dependency
//        project.getDependencies().add(restCompatTestCompileConfig.getName(), compatModuleDependency);
////


        project.getPluginManager().withPlugin("idea", p -> {
            IdeaModel idea = project.getExtensions().getByType(IdeaModel.class);
            idea.getModule().setSourceDirs(restCompatSourceSet.getJava().getSrcDirs());

        });
//        project.getPluginManager().withPlugin("eclipse", p -> {
//            EclipseModel eclipse = project.getExtensions().getByType(EclipseModel.class);
//            List<SourceSet> eclipseSourceSets = new ArrayList<>();
//            for (SourceSet old : eclipse.getClasspath().getSourceSets()) {
//                eclipseSourceSets.add(old);
//            }
//            eclipseSourceSets.add(compatSourceSet);
//            eclipse.getClasspath().setSourceSets(sourceSets);
//            eclipse.getClasspath().getPlusConfigurations().add(runtimeClasspathConfiguration);
//        });

    }
}
