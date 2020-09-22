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

package org.elasticsearch.gradle.testclusters;

import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.gradle.FileSupplier;
import org.elasticsearch.gradle.PropertyNormalization;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Mutable pojo container to store Elasticsearch test cluster configuration.
 */
public class ElasticsearchClusterConfig implements TestClusterConfiguration {

    private int numberOfNodes = -1;
    private Set<String> versions = new HashSet<>();
    private TestDistribution testDistribution;
    private Set<Provider<RegularFile>> plugins1 = new HashSet<>();
    private Set<String> plugins2 = new HashSet<>();
    private Set<Provider<RegularFile>> modules1 = new HashSet<>();
    private Set<String> modules2 = new HashSet<>();
    private Pair<String, String> keystore1;
    private Pair<String, Supplier<CharSequence>> keystore2;
    private Pair<String, File> keystore3;
    private Pair<String, Pair<File, PropertyNormalization>> keystore4;
    private Pair<String, FileSupplier> keystore5;
    private String keyStorePassword;
    private Pair<String, List<CharSequence>> cli;
    private Pair<String, String> setting1;
    private Pair<String, Pair<String, PropertyNormalization>> setting2;
    private Pair<String, Supplier<CharSequence>> setting3;
    private Pair<String, Pair<Supplier<CharSequence>, PropertyNormalization>> setting4;
    private Pair<String, String> systemProperty1;
    private Pair<String, Supplier<CharSequence>> systemProperty2;
    private Pair<String, Pair<Supplier<CharSequence>, PropertyNormalization>> systemProperty3;
    private Pair<String, String> environment1;
    private Pair<String, Supplier<CharSequence>> environment2;
    private Pair<String, Pair<Supplier<CharSequence>, PropertyNormalization>> environment3;
    private List<String> jvmArgs;
    private Pair<String, File> extraConfigFile1;
    private Pair<String, Pair<File, PropertyNormalization>> extraConfigFile2;
    private File extraJarFile;
    private Map<String, String> user;
    private Function<String, String> nameCustomization;

    /**
     * Copies itself to a test cluster iff the values have been set.
     *
     * @param cluster The cluster to copy this config to.
     */
    public void copyToCluster(ElasticsearchCluster cluster) {
        if (this.numberOfNodes > 0) {
            cluster.setNumberOfNodes(this.numberOfNodes);
        }
        if (this.testDistribution != null) {
            cluster.setTestDistribution(this.testDistribution);
        }

        this.plugins1.forEach(cluster::plugin);
        this.plugins2.forEach(cluster::plugin);

        this.modules1.forEach(cluster::module);
        this.modules2.forEach(cluster::module);

        this.versions.forEach(cluster::setVersion);

        if (this.keystore1 != null) {
            cluster.keystore(keystore1.getLeft(), keystore1.getRight());
        }
        if (this.keystore2 != null) {
            cluster.keystore(keystore2.getLeft(), keystore2.getRight());
        }
        if (this.keystore3 != null) {
            cluster.keystore(keystore3.getLeft(), keystore3.getRight());
        }
        if (this.keystore4 != null) {
            cluster.keystore(keystore4.getLeft(), keystore4.getRight().getLeft(), keystore4.getRight().getRight());
        }
        if (this.keystore5 != null) {
            cluster.keystore(keystore5.getLeft(), keystore5.getRight());
        }
        if (this.keyStorePassword != null) {
            cluster.keystorePassword(this.keyStorePassword);
        }
        if (this.cli != null) {
            cluster.cliSetup(cli.getLeft(), cli.getRight().toArray(CharSequence[]::new));
        }
        if (this.setting1 != null) {
            cluster.setting(setting1.getLeft(), setting1.getRight());
        }
        if (this.setting2 != null) {
            cluster.setting(setting2.getLeft(), setting2.getRight().getLeft(), setting2.getRight().getRight());
        }
        if (this.setting3 != null) {
            cluster.setting(setting3.getLeft(), setting3.getRight());
        }
        if (this.setting4 != null) {
            cluster.setting(setting4.getLeft(), setting4.getRight().getLeft(), setting4.getRight().getRight());
        }
        if (this.systemProperty1 != null) {
            cluster.systemProperty(this.systemProperty1.getLeft(), this.systemProperty1.getRight());
        }
        if (this.systemProperty2 != null) {
            cluster.systemProperty(this.systemProperty2.getLeft(), this.systemProperty2.getRight());
        }
        if (this.systemProperty3 != null) {
            cluster.systemProperty(this.systemProperty3.getLeft(), this.systemProperty3.getRight().getLeft(), this.systemProperty3.getRight().getRight());
        }
        if (this.environment1 != null) {
            cluster.environment(this.environment1.getLeft(), this.environment1.getRight());
        }
        if (this.environment2 != null) {
            cluster.environment(this.environment2.getLeft(), this.environment2.getRight());
        }
        if (this.environment3 != null) {
            cluster.environment(this.environment3.getLeft(), this.environment3.getRight().getLeft(), this.environment3.getRight().getRight());
        }
        if (this.jvmArgs != null) {
            cluster.jvmArgs(this.jvmArgs.toArray(String[]::new));
        }
        if (this.extraConfigFile1 != null) {
            cluster.extraConfigFile(this.extraConfigFile1.getLeft(), this.extraConfigFile1.getRight());
        }
        if (this.extraConfigFile2 != null) {
            cluster.extraConfigFile(this.extraConfigFile2.getLeft(), this.extraConfigFile2.getRight().getLeft(), this.extraConfigFile2.getRight().getRight());
        }
        if (this.extraJarFile != null) {
            cluster.extraJarFile(this.extraJarFile);
        }
        if (this.user != null) {
            cluster.user(this.user);
        }
        if (this.nameCustomization != null) {
            cluster.setNameCustomization(this.nameCustomization);
        }
    }

    public void setNumberOfNodes(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    @Override
    public void setVersion(String version) {
        this.versions.add(version);
    }

    @Override
    public void setVersions(List<String> versions) {
        this.versions.addAll(versions);
    }

    @Override
    public void setTestDistribution(TestDistribution distribution) {
        this.testDistribution = distribution;
    }

    @Override
    public void plugin(Provider<RegularFile> plugin) {
        this.plugins1.add(plugin);
    }

    @Override
    public void plugin(String pluginProjectPath) {
        this.plugins2.add(pluginProjectPath);
    }

    @Override
    public void module(Provider<RegularFile> module) {
        this.modules1.add(module);
    }

    @Override
    public void module(String moduleProjectPath) {
        this.modules2.add(moduleProjectPath);
    }

    @Override
    public void keystore(String key, String value) {
        this.keystore1 = Pair.of(key, value);
    }

    @Override
    public void keystore(String key, Supplier<CharSequence> valueSupplier) {
        this.keystore2 = Pair.of(key, valueSupplier);
    }

    @Override
    public void keystore(String key, File value) {
        this.keystore3 = Pair.of(key, value);
    }

    @Override
    public void keystore(String key, File value, PropertyNormalization normalization) {
        this.keystore4 = Pair.of(key, Pair.of(value, normalization));
    }

    @Override
    public void keystore(String key, FileSupplier valueSupplier) {
        this.keystore5 = Pair.of(key, valueSupplier);
    }

    @Override
    public void keystorePassword(String password) {
        this.keyStorePassword = password;
    }

    @Override
    public void cliSetup(String binTool, CharSequence... args) {
        this.cli = Pair.of(binTool, List.of(args));
    }

    @Override
    public void setting(String key, String value) {
        this.setting1 = Pair.of(key, value);
    }

    @Override
    public void setting(String key, String value, PropertyNormalization normalization) {
        this.setting2 = Pair.of(key, Pair.of(value, normalization));
    }

    @Override
    public void setting(String key, Supplier<CharSequence> valueSupplier) {
        this.setting3 = Pair.of(key, valueSupplier);
    }

    @Override
    public void setting(String key, Supplier<CharSequence> valueSupplier, PropertyNormalization normalization) {
        this.setting4 = Pair.of(key, Pair.of(valueSupplier, normalization));
    }

    @Override
    public void systemProperty(String key, String value) {
        this.systemProperty1 = Pair.of(key, value);
    }

    @Override
    public void systemProperty(String key, Supplier<CharSequence> valueSupplier) {
        this.systemProperty2 = Pair.of(key, valueSupplier);
    }

    @Override
    public void systemProperty(String key, Supplier<CharSequence> valueSupplier, PropertyNormalization normalization) {
        this.systemProperty3 = Pair.of(key, Pair.of(valueSupplier, normalization));
    }

    @Override
    public void environment(String key, String value) {
        this.environment1 = Pair.of(key, value);
    }

    @Override
    public void environment(String key, Supplier<CharSequence> valueSupplier) {
        this.environment2 = Pair.of(key, valueSupplier);
    }

    @Override
    public void environment(String key, Supplier<CharSequence> valueSupplier, PropertyNormalization normalization) {
        this.environment3 = Pair.of(key, Pair.of(valueSupplier, normalization));
    }

    @Override
    public void jvmArgs(String... values) {
        this.jvmArgs = List.of(values);
    }

    @Override
    public void extraConfigFile(String destination, File from) {
        this.extraConfigFile1 = Pair.of(destination, from);
    }

    @Override
    public void extraConfigFile(String destination, File from, PropertyNormalization normalization) {
        this.extraConfigFile2 = Pair.of(destination, Pair.of(from, normalization));
    }

    @Override
    public void extraJarFile(File from) {
        this.extraJarFile = from;
    }

    @Override
    public void user(Map<String, String> userSpec) {
        this.user = Map.copyOf(userSpec);
    }

    @Override
    public void setNameCustomization(Function<String, String> nameSupplier) {
        this.nameCustomization = nameSupplier;
    }

    @Override
    public void freeze() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restart() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHttpSocketURI() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTransportPortURI() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAllHttpSocketURI() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAllTransportPortURI() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop(boolean tailLogs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProcessAlive() {
        throw new UnsupportedOperationException();
    }
}
