package org.elasticsearch.common.xcontent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marker interfaces to generate an XContent model. File must be JSON and path must point to a valid JSON schema object.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Repeatable(value = GeneratedModels.class)
public @interface GeneratedModel {

    String value();

}
