package org.elasticsearch.common.xcontent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marker interfaces to generate an model. File must be JSON and path must point to a valid JSON schema object.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface GeneratedModels {
    GeneratedModel[] value() default {};
}
