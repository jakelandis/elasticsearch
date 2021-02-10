/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.test.rest.transform.warnings;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.gradle.test.rest.transform.RestTestTransform;
import org.elasticsearch.gradle.test.rest.transform.RestTestTransformByParentObject;
import org.elasticsearch.gradle.test.rest.transform.feature.FeatureInjector;

import java.util.List;

/**
 * A {@link RestTestTransform} that injects HTTP headers into a REST test. This includes adding the necessary values to the "do" section
 * as well as adding headers as a features to the "setup" and "teardown" sections.
 */
public class InjectAllowedWarnings extends FeatureInjector implements RestTestTransformByParentObject {

    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);

    private final List<String> allowedWarnings;

    /**
     * @param allowedWarnings The allowed warnings to inject
     */
    public InjectAllowedWarnings(List<String> allowedWarnings) {
        this.allowedWarnings = allowedWarnings;
    }

    @Override
    public void transformTest(ObjectNode doNodeParent) {
        ObjectNode doNodeValue = (ObjectNode) doNodeParent.get(getKeyToFind());
        ArrayNode arrayWarnings = (ArrayNode) doNodeValue.get("allowed_warnings");
        if (arrayWarnings == null) {
            arrayWarnings = new ArrayNode(jsonNodeFactory);
            doNodeValue.set("allowed_warnings", arrayWarnings);
        }
        allowedWarnings.forEach(arrayWarnings::add);
    }

    @Override
    public String getKeyToFind() {
        return "do";
    }

    @Override
    public String getSkipFeatureName() {
        return "allowed_warnings";
    }
}
