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
import java.util.List;
import java.util.concurrent.locks.Lock;

public class YamlRestCompatibilityExtension {

    final IncludeExclude gradleProject;
    final IncludeExclude tests;
    final ListProperty<Version> versions;


    @Inject
    public YamlRestCompatibilityExtension(ObjectFactory objects) {
        gradleProject = new IncludeExclude(objects);
        tests = new IncludeExclude(objects);
        versions = objects.listProperty(Version.class);
        //TODO: support configurable versions
        versions.add(BuildParams.getBwcVersions().getLatestMinor());
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
        private final ListProperty<String> includeOnly;
        private final ListProperty<String> excludeOnly;
        private ListProperty<String> activeInclude = null;
        private ListProperty<String> activeExclude = null;


        IncludeExclude(ObjectFactory objects) {
            include = objects.listProperty(String.class);
            exclude = objects.listProperty(String.class);
            includeOnly = objects.listProperty(String.class);
            excludeOnly = objects.listProperty(String.class);
        }

        public synchronized void include(String... include) {
            if (this.includeOnly == activeInclude) {
                throw new IllegalStateException("either include or includeOnly may be defined, but not both");
            }
            this.include.addAll(include);
            activeInclude = this.include;
        }

        public synchronized void exclude(String... exclude) {
            if (this.excludeOnly == activeExclude) {
                throw new IllegalStateException("either exclude or excludeOnly may be defined, but not both");
            }
            this.exclude.addAll(exclude);
            activeExclude = this.exclude;
        }

        public synchronized void includeOnly(String... includeOnly) {
            if (this.include == activeInclude) {
                throw new IllegalStateException("either include or includeOnly may be defined, but not both");
            }
            this.includeOnly.addAll(includeOnly);
            activeInclude = this.includeOnly;
        }

        public synchronized void excludeOnly(String... excludeOnly) {
            if (this.exclude == activeExclude) {
                throw new IllegalStateException("either exclude or excludeOnly may be defined, but not both");
            }
            this.excludeOnly.addAll(excludeOnly);
            this.activeExclude = this.excludeOnly;
        }

        public ListProperty<String> getInclude() {
            return include;
        }

        public ListProperty<String> getExclude() {
            return exclude;
        }

        public ListProperty<String> getIncludeOnly() {
            return includeOnly;
        }

        public ListProperty<String> getExcludeOnly() {
            return excludeOnly;
        }
    }


}

