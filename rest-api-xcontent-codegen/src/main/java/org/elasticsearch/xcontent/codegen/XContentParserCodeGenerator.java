package org.elasticsearch.xcontent.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.GeneratedXContentParser;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.lang.model.SourceVersion.RELEASE_11;

//import org.elasticsearch.http.ModeledHttpResponse;

@SupportedSourceVersion(RELEASE_11)
@SupportedAnnotationTypes("org.elasticsearch.common.xcontent.GeneratedXContentParser")
@SupportedOptions("api.root")
public class XContentParserCodeGenerator extends AbstractProcessor {

    static final String ROOT_OBJECT_NAME = "__ROOT__";

    private static Set<String> VALID_OBJECT_KEYS = Set.of("description", "type", "properties");
    private static Set<String> VALID_PRIMITIVE_KEYS = Set.of("description", "type", "$ref");
    private static Set<String> VALID_ROOT_OBJECT_KEYS = Stream.concat(VALID_OBJECT_KEYS.stream(), Stream.of("$id", "$schema", "definitions")).collect(Collectors.toSet());
    private static Set<String> VALID_ARRAY_KEYS = Set.of("description", "type", "items");
    private static Set<String> VALID_ARRAY_ITEMS_KEYS = Set.of("description", "type", "$ref");
    //TODO more validation

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "*********** Starting processing here" + processingEnv.getOptions().get("api.root"));

        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "*********************************************3");
        //TODO: handle the v7 generations
        for (Element element : roundEnv.getElementsAnnotatedWith(GeneratedXContentParser.class)) {
            try {
                GeneratedXContentParser annotation = element.getAnnotation(GeneratedXContentParser.class);
                ClassName originalClassName = ClassName.bestGuess(element.asType().toString());
                ClassName generateClassName = ClassName.get(originalClassName.packageName() + ".generated", "Generated" + originalClassName.simpleName());
                Path apiRootPath = new File(processingEnv.getOptions().get("api.root")).toPath();
                Path jsonPath = apiRootPath.resolve(annotation.file());
                String schemaPath = annotation.schemaPath();

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating " + generateClassName + " from " + originalClassName + " from " + jsonPath.toAbsolutePath().toString() + " schemaPath=" + schemaPath);

                try (InputStream in = Files.newInputStream(jsonPath);
                     Writer writer = processingEnv.getFiler().createSourceFile(generateClassName.reflectionName()).openWriter()) {
                    JavaFile javaFile = generateClass(in, generateClassName, schemaPath);
                    javaFile.writeTo(writer);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }


    public JavaFile generateClass(InputStream json, ClassName className, String jsonPathToSchemaObject) throws IOException {

        JsonNode schemaObject = findSchemaObject(json, jsonPathToSchemaObject);
        XContentClassBuilder builder = XContentClassBuilder.newToXContentClassBuilder();
        JsonNode definitionNode = schemaObject.get("definitions");
        if (definitionNode != null) {
            traverseDefinitions(definitionNode, builder);
        }
        traverse(schemaObject, ROOT_OBJECT_NAME, builder);

        return XContentClassBuilder.build(className.packageName(), className.simpleName(), builder);
    }

    private JsonNode findSchemaObject(InputStream json, String jsonPathToSchemaObject) throws IOException {
        //tODO: better validation
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaObject = mapper.readTree(json);
        if(ROOT_OBJECT_NAME.equals(jsonPathToSchemaObject)){
            return schemaObject;
        }
        String[] paths = jsonPathToSchemaObject.split("\\.");
        for(String path : paths){
            schemaObject = schemaObject.get(path);
        }
        return schemaObject;
    }
    private void traverseDefinitions(JsonNode definitionNode, XContentClassBuilder builder) {
        //todo: support in file definitions

        definitionNode.fields().forEachRemaining(f -> {
            validateObject(f.getValue(), false);
            traverse(f.getValue(), f.getKey(), addObject(f.getKey(), builder, false));
        });

    }

    private void traverse(JsonNode node, String key, XContentClassBuilder builder) {
        JsonNode typeNode = node.get("type");
        if ("object".equals(typeNode.asText())) {
            validateObject(node, ROOT_OBJECT_NAME.equals(key));
            JsonNode propertiesNode = node.get("properties");
            propertiesNode.fields().forEachRemaining(f -> {

                //primitive reference
                JsonNode primitiveReference = f.getValue().get("$ref");
                //not a reference
                JsonNode nestedTypeNode = f.getValue().get("type");
                if (nestedTypeNode != null) {
                    if ("array".equals(nestedTypeNode.asText())) {
                        validateArray(f.getValue());

                        //primitive array
                        if (f.getValue().get("items").get("type") != null) {
                            addArray(f.getKey(), f.getValue().get("items").get("type").asText(), builder, false);
                        } else { //an array of object references
                            String reference = getRefName(f.getValue().get("items").get("$ref").asText());
                            addArray(f.getKey(), reference, builder, true);
                        }
                    } else if ("object".equals(nestedTypeNode.asText())) {
                        validateObject(f.getValue(), ROOT_OBJECT_NAME.equals(key));
                        traverse(f.getValue(), f.getKey(), addObject(f.getKey(), builder, true));
                    } else {

                        validatePrimitive(f.getValue());
                        String type = nestedTypeNode.asText();
                        addPrimitive(f.getKey(), type, builder, false);
                    }
                } else if (primitiveReference != null) {
                    //handle primitive type
                    String reference = getRefName(primitiveReference.asText());
                    addPrimitive(f.getKey(), reference, builder, true);

                } else {
                    throw new IllegalStateException("Found unsupported object [" + f.getValue().toString() + "]");

                }
            });
        }
    }

    private void validatePrimitive(JsonNode node) {
        assert node.isObject(); //we are validating the node object that contains the "type" : "array" or "$ref" :
        node.deepCopy().fieldNames().forEachRemaining(name -> {
            if (VALID_PRIMITIVE_KEYS.contains(name) == false) {
                throw new IllegalStateException("Found unsupported key name [" + name + "] in object " + node.toString());
            }
        });
    }

    private void validateObject(JsonNode node, boolean isRoot) {
        assert node.isObject();
        node.deepCopy().fieldNames().forEachRemaining(name -> {
            Set<String> validObjectKeys = VALID_OBJECT_KEYS;
            if (isRoot) {
                validObjectKeys = VALID_ROOT_OBJECT_KEYS;
            }
            if (validObjectKeys.contains(name) == false) {
                throw new IllegalStateException("Found unsupported key name [" + name + "] in object " + node.toString());
            }
        });
    }

    private void validateArray(JsonNode node) {
        assert node.isObject(); //we are validating the node object that contains the "type" : "array" or "$ref" :
        node.deepCopy().fieldNames().forEachRemaining(name -> {
            if (VALID_ARRAY_KEYS.contains(name) == false) {
                throw new IllegalStateException("Found unsupported key name [" + name + "] in object " + node.toString());
            }
            if ((node.get("items").get("type") == null && node.get("items").get("$ref") == null)) {
                throw new IllegalStateException("Could not find primitive type or object $ref for array [" + node.toString() + "]");
            }
            //TODO: validate correctness of $ref value by parsing value and ensure that the stupid xpath like thinggy is well shaped.
            node.get("items").fieldNames().forEachRemaining(i -> {
                if (VALID_ARRAY_ITEMS_KEYS.contains(i) == false) {
                    throw new IllegalStateException("Found unsupported array item name [" + i + "] in object " + node.toString());
                }
            });
        });
    }

    private String getRefName(String reference) {
        String[] tokens = tokenReference(reference);
        return tokens[1].substring(tokens[1].lastIndexOf("/") + 1);
    }

    private String[] tokenReference(String reference) {
        assert reference.contains("#");
        String[] tokens = reference.split("#");
        assert tokens.length == 2;
        return tokens;

    }

    private XContentClassBuilder addObject(String type, XContentClassBuilder builder, boolean addToInitialization) {

        //don't add reference classes to the initialization blocks
        if (addToInitialization) {
            addInitializationCode(getClassName(type), type, builder, true, false);
        }
        XContentClassBuilder child = XContentClassBuilder.newToXContentClassBuilder();
        builder.children.add(new Tuple<>(getClassName(type), child));
        return child;
    }

    private void addArray(String field, String type, XContentClassBuilder builder, boolean objectReference) {
        addInitializationCode(getClassName(type), field, builder, objectReference, true);
    }

    private void addPrimitive(String field, String type, XContentClassBuilder builder, boolean objectReference) {
        addInitializationCode(getClassName(type), field, builder, objectReference, false);
    }

    private ClassName getClassName(String type) {
        String _type = type;
        switch (type) {
            case "integer":
                _type = "long"; //In JSON spec "integer" = non-fractional
                break;
            case "number":
                _type = "double";  //In JSON spec "number" = fractional
                break;
        }
        return ClassName.get("", CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, _type));
    }

    //Adds the Constructor and static initialization code to the XContentClassBuilder.
    private void addInitializationCode(ClassName className, String field, XContentClassBuilder builder, boolean isObject, boolean isArray) {
        // ConstructingObjectParser arguments
        if (isArray) {
            builder.lambdas.add(CodeBlock.builder().add("(List<" + className.simpleName() + ">) a[$L]", builder.parserPosition.incrementAndGet()).build());
        } else {
            builder.lambdas.add(CodeBlock.builder().add("(" + className.simpleName() + ") a[$L]", builder.parserPosition.incrementAndGet()).build());

        }
        // PARSER.declare
        if (isObject && isArray == false) {
            builder.staticInitializerBuilder.add("PARSER.declareObject(ConstructingObjectParser.constructorArg(), " + className.simpleName() + ".PARSER, new $T($S));\n", ParseField.class, field);
        } else if (isObject & isArray) {
            builder.staticInitializerBuilder.add("PARSER.declareObjectArray(ConstructingObjectParser.constructorArg(), " + className.simpleName() + ".PARSER, new $T($S));\n", ParseField.class, field);
        } else if (isArray) {
            builder.staticInitializerBuilder.add("PARSER.declare" + className.simpleName() + "Array(ConstructingObjectParser.constructorArg(), new $T($S));\n", ParseField.class, field);
        } else {
            builder.staticInitializerBuilder.add("PARSER.declare" + className.simpleName() + "(ConstructingObjectParser.constructorArg(), new $T($S));\n", ParseField.class, field);
        }

        TypeName typeName = className;
        if (isArray) {
            typeName = ParameterizedTypeName.get(ClassName.get("java.util", "List"), className);
        }

        builder.constructorBuilder.addParameter(typeName, field).addStatement("this.$N = $N", field, field);
        builder.fields.add(FieldSpec.builder(typeName, field).addModifiers(Modifier.PUBLIC, Modifier.FINAL).build());
        builder.toXContentMethodBuilder.addStatement("builder.field($S," + field + ")", field);
    }


}



