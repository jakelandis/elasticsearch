package org.elasticsearch.http.api.main;

import org.elasticsearch.http.ModeledHttpResponse;

/**
 * Marker interface to generate the x-content serialization
 */
@ModeledHttpResponse(previous = "main/v7/response.json", current = "main/v8/response.json")
public interface MainResponseModel {
}

//for intellij need to enable processor annontation processing for this module and mark build-idea/classes/main/generated as sources root
