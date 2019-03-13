/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.ingest;

import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.TemplateScript;

import java.util.Map;

public class PipelineProcessor extends AbstractProcessor {

    public static final String TYPE = "pipeline";

    private final TemplateScript.Factory pipelineNameTemplate;

    //TODO: is this safe ?
    private String pipelineName;
    private final IngestService ingestService;

    private PipelineProcessor(String tag, TemplateScript.Factory pipelineNameTemplate, IngestService ingestService) {
        super(tag);
        this.pipelineNameTemplate = pipelineNameTemplate;

        this.ingestService = ingestService;
    }

    @Override
    public IngestDocument execute(IngestDocument ingestDocument) throws Exception {
        Map<String, Object> model = ingestDocument.createTemplateModel();
        //is this safe and performant ?
        pipelineName = pipelineNameTemplate.newInstance(model).execute();

        Pipeline pipeline = ingestService.getPipeline(pipelineName);
        if (pipeline == null) {
            throw new IllegalStateException("Pipeline processor configured for non-existent pipeline [" + pipelineName + ']');
        }
        return ingestDocument.executePipeline(pipeline);
    }

    Pipeline getPipeline(){
        return ingestService.getPipeline(pipelineName);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    String getPipelineName() {
        return pipelineName;
    }

    public static final class Factory implements Processor.Factory {

        private final IngestService ingestService;
        private final ScriptService scriptService;


        public Factory(IngestService ingestService, ScriptService scriptService) {
            this.ingestService = ingestService;
            this.scriptService = scriptService;
        }

        @Override
        public PipelineProcessor create(Map<String, Processor.Factory> registry, String processorTag,
            Map<String, Object> config) throws Exception {
            String pipeline =
                ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "name");

            TemplateScript.Factory pipelineNameTemplate = ConfigurationUtils.compileTemplate(TYPE, processorTag,
                "pipeline", pipeline, scriptService);

            return new PipelineProcessor(processorTag, pipelineNameTemplate, ingestService);
        }
    }
}
