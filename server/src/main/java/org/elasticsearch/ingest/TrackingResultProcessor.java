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

import org.elasticsearch.action.ingest.SimulateProcessorResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Processor to be used within Simulate API to keep track of processors executed in pipeline.
 */
public class TrackingResultProcessor implements Processor {

    private Processor actualProcessor;
    private final List<SimulateProcessorResult> processorResultList;
    private final boolean ignoreFailure;

    TrackingResultProcessor(boolean ignoreFailure, Processor actualProcessor, List<SimulateProcessorResult> processorResultList) {
        this.ignoreFailure = ignoreFailure;
        this.processorResultList = processorResultList;
        this.actualProcessor = actualProcessor;
    }

    @Override
    public IngestDocument execute(IngestDocument ingestDocument) throws Exception {
        try {
            if (actualProcessor instanceof ConditionalProcessor) {
                ConditionalProcessor conditionalProcessor = (ConditionalProcessor) actualProcessor;
                if (conditionalProcessor.evaluate(ingestDocument) == false) {
                    return ingestDocument;
                }

                if (conditionalProcessor.getProcessor() instanceof PipelineProcessor) {
                    actualProcessor = conditionalProcessor.getProcessor();
                }
            }

            if (actualProcessor instanceof PipelineProcessor) {
                PipelineProcessor pipelineProcessor = ((PipelineProcessor) actualProcessor);
                Pipeline pipeline = pipelineProcessor.getPipeline();
                CompoundProcessor verbosePipelineProcessor = decorate(pipeline.getCompoundProcessor(), processorResultList);
                Pipeline verbosePipeline = new Pipeline(pipeline.getId(), pipeline.getDescription(), pipeline.getVersion(),
                    verbosePipelineProcessor);
                ingestDocument.executePipeline(verbosePipeline);

            } else
                {
                actualProcessor.execute(ingestDocument);
                processorResultList.add(new SimulateProcessorResult(actualProcessor.getTag(), new IngestDocument(ingestDocument)));
            }
        } catch (Exception e) {
            if (ignoreFailure) {
                processorResultList.add(new SimulateProcessorResult(actualProcessor.getTag(), new IngestDocument(ingestDocument), e));
            } else {
                processorResultList.add(new SimulateProcessorResult(actualProcessor.getTag(), e));
            }
            throw e;
        }
        return ingestDocument;
    }

    @Override
    public String getType() {
        return actualProcessor.getType();
    }

    @Override
    public String getTag() {
        return actualProcessor.getTag();
    }

    public static void checkForCycles(Pipeline pipeline, Set<PipelineProcessor> pipelinesSeen) {
        for (Processor processor : pipeline.getCompoundProcessor().getProcessors()) {
            if(processor instanceof ConditionalProcessor){
                processor = ((ConditionalProcessor)processor).getProcessor();
            }
            if (processor instanceof PipelineProcessor) {
                PipelineProcessor pipelineProcessor = ((PipelineProcessor) processor);
                if (pipelinesSeen.add(pipelineProcessor) == false) {
                    throw new IllegalStateException("Possible cycle detected between pipelines [" + pipelineProcessor.getPipeline().getId() + "] and [" + pipeline.getId() + "]");
                }
                checkForCycles(pipelineProcessor.getPipeline(), pipelinesSeen);
                pipelinesSeen.remove(pipelineProcessor);
            } else if (processor instanceof CompoundProcessor) {
                checkForCycles(pipeline, pipelinesSeen);
            }
        }
        for (Processor processor : pipeline.getCompoundProcessor().getOnFailureProcessors()) {
            if(processor instanceof ConditionalProcessor){
                processor = ((ConditionalProcessor)processor).getProcessor();
            }
            if (processor instanceof PipelineProcessor) {
                PipelineProcessor pipelineProcessor = ((PipelineProcessor) processor);
                if (pipelinesSeen.add(pipelineProcessor) == false) {
                    throw new IllegalStateException("Possible cycle detected between pipelines [" + pipelineProcessor.getPipeline().getId() + "] and [" + pipeline.getId() + "]");
                }
                checkForCycles(pipelineProcessor.getPipeline(), pipelinesSeen);
                pipelinesSeen.remove(pipelineProcessor);
            } else if (processor instanceof CompoundProcessor) {
                checkForCycles(pipeline, pipelinesSeen);
            }
        }
    }

    public static CompoundProcessor decorate(CompoundProcessor compoundProcessor, List<SimulateProcessorResult> processorResultList) {
        List<Processor> processors = new ArrayList<>(compoundProcessor.getProcessors().size());
        for (Processor processor : compoundProcessor.getProcessors()) {
            if (processor instanceof CompoundProcessor) {
                processors.add(decorate((CompoundProcessor) processor, processorResultList));
            } else {
                processors.add(new TrackingResultProcessor(compoundProcessor.isIgnoreFailure(), processor, processorResultList));
            }
        }
        List<Processor> onFailureProcessors = new ArrayList<>(compoundProcessor.getProcessors().size());
        for (Processor processor : compoundProcessor.getOnFailureProcessors()) {
            if (processor instanceof CompoundProcessor) {
                onFailureProcessors.add(decorate((CompoundProcessor) processor, processorResultList));
            } else {
                onFailureProcessors.add(new TrackingResultProcessor(compoundProcessor.isIgnoreFailure(), processor, processorResultList));
            }
        }
        return new CompoundProcessor(compoundProcessor.isIgnoreFailure(), processors, onFailureProcessors);
    }
}

