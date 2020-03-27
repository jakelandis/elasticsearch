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

package org.elasticsearch.gradle.precommit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.elasticsearch.gradle.util.Util;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.internal.Factory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ValidateRestSpecTask extends DefaultTask {

    private static final String SCHEMA_PROJECT = ":rest-api-spec";
    private static final String DOUBLE_STARS = "**"; // checkstyle thinks this is an empty javadoc statement, so string concat instead
    private static final String JSON_SPEC_PATTERN_INCLUDE = DOUBLE_STARS + "/rest-api-spec/api/" + DOUBLE_STARS + "/*.json";
    private static final String JSON_SPEC_PATTERN_EXCLUDE = DOUBLE_STARS + "/_common.json";
    private static final String JSON_SCHEMA_PATTERN = DOUBLE_STARS + "/schema.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    @SkipWhenEmpty
    @InputFiles
    public FileTree getInputDir() {
        //if null results in in NO-SOURCE
        return Util.getJavaTestAndMainSourceResources(getProject(),
            Util.filterByPatterns(getPatternSetFactory().create(), Collections.singletonList(JSON_SPEC_PATTERN_INCLUDE),
                Collections.singletonList(JSON_SPEC_PATTERN_EXCLUDE)));
    }

    @Inject
    protected Factory<PatternSet> getPatternSetFactory() {
        throw new UnsupportedOperationException();
    }

    @TaskAction
    public void validate() {
        JsonSchema jsonSchema = readJsonSchema();
        FileTree specs = getInputDir();
        for (File file : specs.getFiles()) {
            getLogger().info("Validating JSON spec [{}]", file.getName()); //TODO: debug
            Set<ValidationMessage> validationMessages = jsonSchema.validate(fileToJsonNode(file));
            for (ValidationMessage validationMessage : validationMessages) {
                System.out.println(validationMessage.getMessage()); //TODO: generate report
            }
        }
    }

    //TODO: should the location be configurable ?
    private JsonSchema readJsonSchema() {
        FileTree jsonSchemas = Util.getJavaMainSourceResources(getProject().findProject(SCHEMA_PROJECT),
            Util.filterByPatterns(getPatternSetFactory().create(), Collections.singletonList(JSON_SCHEMA_PATTERN), null));
        if (jsonSchemas == null || jsonSchemas.getFiles().size() != 1) {
            throw new IllegalStateException(
                String.format("Could not find the schema file from glob pattern [%s] and project [%s] for JSON spec validation",
                    JSON_SCHEMA_PATTERN, SCHEMA_PROJECT));
        }
        File jsonSchema = jsonSchemas.iterator().next();
        try {
            SchemaValidatorsConfig config = new SchemaValidatorsConfig();
//TODO: do we need any special config ?
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            return factory.getSchema(fileToJsonNode(jsonSchema), config);
        } catch (Exception e) {
            throw new RuntimeException(e); //TODO
        }
    }

    private JsonNode fileToJsonNode(File file) {
        try {
            return mapper.readTree(file);
        } catch (Exception e) {
            throw new RuntimeException(e); //TODO
        }
    }
}
