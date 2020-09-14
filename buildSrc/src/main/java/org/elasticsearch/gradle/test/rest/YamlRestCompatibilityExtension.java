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

import org.elasticsearch.gradle.Version;
import org.elasticsearch.gradle.info.BuildParams;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class YamlRestCompatibilityExtension {

    final IncludeExclude gradleProject;
    final IncludeExclude tests;
    //TODO: make this configurable
    List<Version> versions = Collections.singletonList(BuildParams.getBwcVersions().getLatestMinor());


    @Inject
    public YamlRestCompatibilityExtension(ObjectFactory objects) {
        gradleProject = new IncludeExclude(objects);
        tests = new IncludeExclude(objects);
    }

    void gradleProject(Action<? super IncludeExclude> includeExclude) {
        includeExclude.execute(gradleProject);
    }

    void tests(Action<? super IncludeExclude> includeExclude) {
        includeExclude.execute(tests);
    }

    static class IncludeExclude {

        private final ListProperty<String> include;
        private final ListProperty<String> exclude;

        IncludeExclude(ObjectFactory objects) {
            include = objects.listProperty(String.class);
            exclude = objects.listProperty(String.class);
        }

        public void include(String... include) {
            this.include.addAll(include);
        }

        public void exclude(String... exclude) {

            this.exclude.addAll(exclude);
        }

        public ListProperty<String> getInclude() {
            return include;
        }

        public ListProperty<String> getExclude() {
            return exclude;
        }
    }
}

