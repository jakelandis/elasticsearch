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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.lang.model.SourceVersion.RELEASE_11;


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

                Path apiRootPath = new File(processingEnv.getOptions().get("api.root")).toPath();
                Path jsonPath = apiRootPath.resolve(annotation.file());
                String schemaPath = annotation.schemaPath();

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating somehting something..." + originalClassName + " from " + jsonPath.toAbsolutePath().toString() + " schemaPath=" + schemaPath);
                //ClassName className = jsonPath.getFileName().toString().split("\\.")[0];
                ClassName className = ClassName.bestGuess("com.example.Foo"); //fixme
                try (Writer writer = processingEnv.getFiler().createSourceFile(className.reflectionName()).openWriter()) {
                    HashSet<JavaFile> sourceFiles = new HashSet<>();
                    generateClasses(className, jsonPath, schemaPath, sourceFiles, apiRootPath);
                    // javaFile.writeTo(System.out);
                    ///gradlew :rest-api-xcontent-parsers:compileJava --stacktrace
                    for (JavaFile sourceFile : sourceFiles) {
                        sourceFile.writeTo(writer);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }


    public void generateClasses(ClassName className, Path jsonPath, String jPath, Set<JavaFile> sourceFiles, Path apiRootPath) throws IOException {
        try (InputStream in = Files.newInputStream(jsonPath)) {
            JsonNode schemaObject = findObjectToParse(in, jPath);
            XContentClassBuilder builder = XContentClassBuilder.newToXContentClassBuilder();

            Set<String> externalReferences = new HashSet<>();
            //handle any inline definitions that will end up as inner classes
            JsonNode definitionNode = schemaObject.get("definitions");
            if (definitionNode != null) {
                traverseInlineDefinitions(definitionNode, builder, externalReferences);
            }

            traverse(schemaObject, ROOT_OBJECT_NAME, builder, externalReferences);
            for (String externalRef : externalReferences) {
                Path externalRefPath = apiRootPath.resolve(Paths.get(externalRef.split("#")[0])); //todo more validation here?
                Tuple<String, String> packageAndClass = parseExternalReference(externalRef);
                generateClasses(getClassName(packageAndClass), externalRefPath, externalRef.split("#")[1], sourceFiles, apiRootPath);
            }

            sourceFiles.add(XContentClassBuilder.build(className.packageName(), className.simpleName(), builder));
        }
    }


    private JsonNode findObjectToParse(InputStream json, String jPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        if (ROOT_OBJECT_NAME.equals(jPath)) {
            return node;
        }
        String[] paths = {};
        if (jPath.contains(".")) {
            paths = jPath.split("\\.");
        } else if (jPath.contains("/")) {
            paths = jPath.split("/");
        }
        for (String path : paths) {
            if (path.isEmpty() == false) {
                node = node.get(path);
            }
        }
        return node;
    }

    private void traverseInlineDefinitions(JsonNode definitionNode, XContentClassBuilder builder, Set<String> externalReferences) {
        definitionNode.fields().forEachRemaining(f -> {
            validateObject(f.getValue(), false);
            addField(f.getKey(), getClassName("", f.getKey()), builder, true);
            traverse(f.getValue(), f.getKey(), addObject(f.getKey(), getClassName("", f.getKey()), builder, false), externalReferences);
        });
    }

    private void traverse(JsonNode node, String key, XContentClassBuilder builder, Set<String> externalReferences) {
        JsonNode typeNode = node.get("type");
        if ("object".equals(typeNode.asText())) {
            validateObject(node, ROOT_OBJECT_NAME.equals(key));
            JsonNode propertiesNode = node.get("properties");
            if (propertiesNode != null) { //support for empty objects
                propertiesNode.fields().forEachRemaining(f -> {
                    //reference to another object
                    JsonNode reference = f.getValue().get("$ref");
                    //not a reference
                    JsonNode nestedTypeNode = f.getValue().get("type");
                    if (nestedTypeNode != null) {
                        if ("array".equals(nestedTypeNode.asText())) {
                            validateArray(f.getValue());
                            //normal array
                            if (f.getValue().get("items").get("type") != null) {
                                addArray(f.getKey(), getClassName("", f.getValue().get("items").get("type").asText()), builder, false);
                            } else { //an array of object references
                                reference = f.getValue().get("items").get("$ref");
                                String refText = reference.asText();
                                if (isExternalReference(refText)) {
                                    externalReferences.add(refText);
                                    addArray(f.getKey(), getClassName(parseExternalReference(refText)), builder, true);
                                } else {
                                    addArray(f.getKey(), getClassName(parseInlineReference(refText)), builder, true);
                                }
                            }
                        } else if ("object".equals(nestedTypeNode.asText())) {
                            validateObject(f.getValue(), ROOT_OBJECT_NAME.equals(key));

                            traverse(f.getValue(), f.getKey(), addObject(f.getKey(), getClassName("", f.getKey()), builder, true), externalReferences);
                        } else {

                            validatePrimitive(f.getValue());
                            String type = nestedTypeNode.asText();
                            addField(f.getKey(), getClassName("", type), builder, false);
                        }
                    } else if (reference != null) {
                        String refText = reference.asText();
                        if (isExternalReference(refText)) {
                            externalReferences.add(refText); //used to generate the external class
                            addField(f.getKey(), getClassName(parseExternalReference(refText)), builder, true);
                        } else {
                            addField(f.getKey(), getClassName(parseInlineReference(refText)), builder, true);
                        }
                    } else {
                        throw new IllegalStateException("Found unsupported object [" + f.getValue().toString() + "]");
                    }
                });
            }
        }
    }

    private boolean isExternalReference(String reference) {
        return reference.contains(".json") && reference.contains("#");
    }

    // "$ref": "ilm/phases/phases.json#/hot"
    // package = ilm.phases
    // class = phases
    private Tuple<String, String> parseExternalReference(String externalReference) {
        assert isExternalReference(externalReference);
        String[] tokens = externalReference.split("#");
        assert tokens.length == 2;
        String[] parts = tokens[0].split(".json")[0].split("/");
        String nameOfClass = parts[parts.length - 1];
        String packageName = Arrays.stream(parts).limit(parts.length - 1).collect(Collectors.joining("."));
        return new Tuple<>(packageName, formatClassName(nameOfClass));
    }

    private Tuple<String, String> parseInlineReference(String inlineReference) {
        assert inlineReference.contains("#");
        String[] tokens = inlineReference.split("#");
        assert tokens.length == 2;
        return new Tuple("", tokens[1].substring(tokens[1].lastIndexOf("/") + 1));
    }

    private XContentClassBuilder addObject(String field, ClassName className, XContentClassBuilder builder, boolean addToInitialization) {
        //don't add inline reference classes to the initialization blocks
        if (addToInitialization) {
            addInitializationCode(className, field, builder, true, false);
        }
        XContentClassBuilder child = XContentClassBuilder.newToXContentClassBuilder();
        builder.children.add(new Tuple<>(className, child));
        return child;
    }

    private void addArray(String field, ClassName className, XContentClassBuilder builder, boolean objectReference) {
        addInitializationCode(className, field, builder, objectReference, true);
    }

    private void addField(String field, ClassName className, XContentClassBuilder builder, boolean objectReference) {
        addInitializationCode(className, field, builder, objectReference, false);
    }

    private ClassName getClassName(Tuple<String, String> packageAndClassName) {
        return getClassName(packageAndClassName.v1(), packageAndClassName.v2());
    }

    ClassName getClassName(String packageName, String className) {
        String pn = packageName;
        switch (className) {
            case "integer":
                className = "Long"; //In JSON spec "integer" = non-fractional
                break;
            case "number":
                className = "Double";  //In JSON spec "number" = fractional
                break;
            case "boolean":
                className = "Boolean";
                break;
            case "string":
                className = "String";
                break;
            case "null":
                throw new UnsupportedOperationException("null type is not supported");
            default:
                className = formatClassName(className);

        }

        return ClassName.get(pn, className);
    }

    private String formatClassName(String nameOfClass) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, nameOfClass);
    }

    //Adds the Constructor and static initialization code to the XContentClassBuilder.
    private void addInitializationCode(ClassName className, String field, XContentClassBuilder builder, boolean isObject, boolean isArray) {
        // ConstructingObjectParser arguments
        if (isArray) {
            builder.lambdas.add(CodeBlock.builder().add("(List<$T>) a[$L]", className, builder.parserPosition.incrementAndGet()).build());
        } else {
            builder.lambdas.add(CodeBlock.builder().add("($T) a[$L]", className, builder.parserPosition.incrementAndGet()).build());
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


}



