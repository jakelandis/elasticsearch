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
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Processor to be used within Simulate API to keep track of processors executed in pipeline.
 */
public final class TrackingResultProcessor implements Processor {

    private final Processor actualProcessor;
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
            actualProcessor.execute(ingestDocument);
            processorResultList.add(new SimulateProcessorResult(actualProcessor.getTag(), new IngestDocument(ingestDocument)));
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

    Processor getActualProcessor() {
        return actualProcessor;
    }

    @Override
    public String getTag() {
        return actualProcessor.getTag();
    }

    public static CompoundProcessor decorate(CompoundProcessor compoundProcessor, List<SimulateProcessorResult> processorResultList,
                                             Set<PipelineProcessor> pipelinesSeen, Tuple<ScriptService, Script> conditionalWrapper) {
        List<Processor> processors = new ArrayList<>(compoundProcessor.getProcessors().size());
        for (Processor processor : compoundProcessor.getProcessors()) {
            //special case of a pipeline processor with a conditional, to show all the processors of a pipeline processor we need to add
            //them to the list of processors, and ignore the actual pipeline processor. However, if there is a the conditional found at the
            //pipeline level (and since we dis-regard the pipeline processor in favor it's processors), we need to wrap each of it's
            //processors with the conditional found on the pipeline processor.
            if (processor instanceof ConditionalProcessor) {
                ConditionalProcessor conditionalProcessor = (ConditionalProcessor) processor;
                if (conditionalProcessor.getProcessor() instanceof PipelineProcessor) {
                    conditionalWrapper = new Tuple<>(conditionalProcessor.getScriptService(),conditionalProcessor.getCondition());
                    //allow this conditional processor to be treated as a pipeline processor
                    //we will add the pipeline conditional to each of the processors below
                    processor = conditionalProcessor.getProcessor();
                }
            }
            if (processor instanceof PipelineProcessor) {
                PipelineProcessor pipelineProcessor = ((PipelineProcessor) processor);
                if (pipelinesSeen.add(pipelineProcessor) == false) {
                    throw new IllegalStateException("Cycle detected for pipeline: " + pipelineProcessor.getPipeline().getId());
                }
                processors.add(decorate(pipelineProcessor.getPipeline().getCompoundProcessor(), processorResultList,
                    pipelinesSeen, conditionalWrapper));
                conditionalWrapper = null;
                pipelinesSeen.remove(pipelineProcessor);
            } else if (processor instanceof CompoundProcessor) {
                processors.add(decorate((CompoundProcessor) processor, processorResultList, pipelinesSeen, conditionalWrapper));
            } else {
                if(conditionalWrapper == null){
                    processors.add(new TrackingResultProcessor(compoundProcessor.isIgnoreFailure(), processor, processorResultList));
                }else {
                    //add the (non-tracking) pipeline conditional around each processor that belongs to a conditional pipeline processor
                    processors.add(
                        new ConditionalProcessor(null, conditionalWrapper.v2(), conditionalWrapper.v1(),
                            new TrackingResultProcessor(compoundProcessor.isIgnoreFailure(), processor, processorResultList)));
                }
            }
        }
        List<Processor> onFailureProcessors = new ArrayList<>(compoundProcessor.getProcessors().size());
        for (Processor processor : compoundProcessor.getOnFailureProcessors()) {
            if (processor instanceof ConditionalProcessor) {
                ConditionalProcessor conditionalProcessor = (ConditionalProcessor) processor;
                if (conditionalProcessor.getProcessor() instanceof PipelineProcessor) {
                    conditionalWrapper = new Tuple<>(conditionalProcessor.getScriptService(),conditionalProcessor.getCondition());
                    processor = conditionalProcessor.getProcessor();
                }
            }
            if (processor instanceof PipelineProcessor) {
                PipelineProcessor pipelineProcessor = ((PipelineProcessor) processor);
                if (pipelinesSeen.add(pipelineProcessor) == false) {
                    throw new IllegalStateException("Cycle detected for pipeline: " + pipelineProcessor.getPipeline().getId());
                }
                onFailureProcessors.add(decorate(pipelineProcessor.getPipeline().getCompoundProcessor(), processorResultList,
                    pipelinesSeen, conditionalWrapper));
                pipelinesSeen.remove(pipelineProcessor);
                conditionalWrapper = null;
            } else if (processor instanceof CompoundProcessor) {
                onFailureProcessors.add(decorate((CompoundProcessor) processor, processorResultList, pipelinesSeen, conditionalWrapper));
            } else {

                if(conditionalWrapper == null){
                    onFailureProcessors.add(
                        new TrackingResultProcessor(compoundProcessor.isIgnoreFailure(), processor, processorResultList));
                }else {
                    //add the (non-tracking) pipeline conditional around each processor from a conditional pipeline processor
                    onFailureProcessors.add(
                        new ConditionalProcessor(null, conditionalWrapper.v2(), conditionalWrapper.v1(),
                            new TrackingResultProcessor(compoundProcessor.isIgnoreFailure(), processor, processorResultList)));
                }

            }
        }
        return new CompoundProcessor(compoundProcessor.isIgnoreFailure(), processors, onFailureProcessors);
    }
}

