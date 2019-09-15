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
@Repeatable(value = GenerateXContentModels.class)
public @interface GenerateXContentModel{
    /**
     * The relative name of the file to use to generate the file.
     * Consumers must provide a base path from which to resolve the relative file. This is likely done through a compiler argument to an
     * annotation processor. The file must be a JSON file that contains a JSON schema object.
     */
    String model();

    /**
     * the package name to generate
     */
    String packageName();

    /**
     * The class name to generate
     */
    String className();
}
