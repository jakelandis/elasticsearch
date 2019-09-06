package org.elasticsearch.common.xcontent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for mark interfaces to generate an XContent parser. File must be JSON and path must point to a valid JSON schema object.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface GeneratedXContentParser {
    /**
     * The relative name of the file to use to generate the file.
     * Consumers must provide a base path from which to resolve the relative file. This is likely done through a compiler argument to an
     * annotation processor. The file must be a JSON file that contains a JSON schema object.
     */
    String file();

    /**
     * The dot delimited path from the root object of JSON to find the JSON schema object used to generate the parser,
     */
    String schemaPath() default "body";
}
