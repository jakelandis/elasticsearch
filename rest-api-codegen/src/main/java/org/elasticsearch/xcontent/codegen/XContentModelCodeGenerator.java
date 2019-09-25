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
import org.elasticsearch.common.xcontent.GeneratedModels;
import org.elasticsearch.common.xcontent.GeneratedModel;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.lang.model.SourceVersion.RELEASE_11;


@SupportedSourceVersion(RELEASE_11)
@SupportedAnnotationTypes({"org.elasticsearch.common.xcontent.GeneratedModel", "org.elasticsearch.common.xcontent.GeneratedModels"})
@SupportedOptions({"spec.root", "package.root", "postfix.name"})
public class XContentModelCodeGenerator extends AbstractProcessor {

    static final String ROOT_OBJECT_NAME = ".";


    // subsets of valid vocabularies at https://github.com/json-schema-org/json-schema-spec/blob/master/schema.json
    // adds in a custom vocabulary of $deprecated TODO: create a meta schema that includes the custom vocabulary (and core)
    private static Set<String> VALID_OBJECT_KEYS = Set.of("$comment", "$deprecated", "description", "type", "properties", "patternProperties");
    private static Set<String> VALID_PRIMITIVE_KEYS = Set.of("$comment", "$deprecated", "description", "type", "$ref");
    private static Set<String> VALID_ROOT_OBJECT_KEYS = Stream.concat(VALID_OBJECT_KEYS.stream(), Stream.of("$comment", "$id", "$schema", "$defs")).collect(Collectors.toSet());
    private static Set<String> VALID_ARRAY_KEYS = Set.of("$comment", "$deprecated", "description", "type", "items");
    private static Set<String> VALID_ARRAY_ITEMS_KEYS = Set.of("$comment", "description", "type", "$ref");
    private static Set<JavaFile> written = new HashSet<>();

   // private String PACKAGE_ROOT = "org.elasticsearch.xcontent.generated";
    static String OBJECT_MAP_ITEM_CLASS_NAME = "ObjectMapItem";
    static String OBJECT_MAP_ITEM_METHOD_NAME = "objectMap";

    //package private for testing
    String packageRoot;

    private String postFixName;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "*********** Starting to process specification from: " + processingEnv.getOptions().get("spec.root") + " with package name:" + processingEnv.getOptions().get("package.root"));

        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWithAny(Set.of(GeneratedModel.class, GeneratedModels.class))) {
            try {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing annotation from: " + element.getSimpleName());
                packageRoot = processingEnv.getOptions().get("package.root");
                postFixName = processingEnv.getOptions().get("postfix.name");
                GeneratedModels generateXContentModels = element.getAnnotation(GeneratedModels.class);
                List<GeneratedModel> models = new ArrayList<>();
                if (generateXContentModels != null) {
                    models.addAll(Arrays.asList(generateXContentModels.value()));
                } else {
                    models.add(element.getAnnotation(GeneratedModel.class));
                }

                for (GeneratedModel model : models) {
                    ClassName originalClassName = ClassName.bestGuess(element.asType().toString());
                    if (element.getKind().isInterface() == false) {
                        throw new IllegalStateException("GeneratedModel(s) must be associated with an Interface for [" + originalClassName + "]");
                    }

                    String relPathToModel = model.value();
                    assert relPathToModel.contains(".json");

                    String[] parts = relPathToModel.split("/");
                    String fileName = parts[parts.length - 1];
                    assert fileName.contains(".json");
                    String nameOfClass = formatClassName(fileName.split("\\.")[0], true);
                    String additionalPackage = parts.length > 1 ? Arrays.stream(parts).limit(parts.length - 1).collect(Collectors.joining(".")) : "";

                    Path specRootPath = new File(processingEnv.getOptions().get("spec.root")).toPath();
                    Path modelRootPath = specRootPath.resolve("model");
                    Path jsonPath = modelRootPath.resolve(model.value());

                    String nameOfPackage = packageRoot + (additionalPackage.isEmpty() ? "" : "." + additionalPackage);

                    ClassName className = ClassName.get(nameOfPackage, nameOfClass);
                    HashSet<JavaFile> sourceFiles = new HashSet<>();
                    generateClasses(className, jsonPath, ROOT_OBJECT_NAME, sourceFiles);
                    for (JavaFile sourceFile : sourceFiles) {
                        //TODO: consider keeping tracking of which classes are already processed in addition to which one have been written.
                        //The first one will be written, but it could be generated (identically) many, many times if it is highly referenced
                        if (written.add(sourceFile)) {
                            try (Writer writer = processingEnv.getFiler().createSourceFile(sourceFile.packageName + "." + sourceFile.typeSpec.name).openWriter()) {
                                sourceFile.writeTo(writer);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }


    private JsonNode findObjectToParse(InputStream json, String jPath) throws IOException {
        assert jPath != null;
        assert json != null;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        assert node != null;
        if (ROOT_OBJECT_NAME.equals(jPath)) {
            return node;
        }
        JsonNode original = node.deepCopy();

        String[] paths = {jPath};
        if (jPath.contains("/")) {
            paths = jPath.split("/");
        }

        for (String path : paths) {
            if (path.isEmpty() == false) {
                node = node.get(path);
            }
            if (node == null) {
                throw new NullPointerException("Could not find the object defined by [" + jPath + "] in object " + original.toString());
            }
        }
        return node;
    }

    public void generateClasses(ClassName className, Path jsonPath, String jPath, Set<JavaFile> sourceFiles) throws IOException {
        if (processingEnv != null) { //possible from tests
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating class " + className + " from " + jsonPath.toAbsolutePath().toString());
        }
        try (InputStream in = Files.newInputStream(jsonPath)) {
            JsonNode objectToParse = findObjectToParse(in, jPath);
            XContentClassBuilder builder = XContentClassBuilder.newToXContentClassBuilder();

            Set<String> externalReferences = new HashSet<>();
            //handle any inline definitions that will end up as inner classes
            JsonNode definitionNode = objectToParse.get("$defs");
            if (definitionNode != null) {
                traverseInlineDefinitions(definitionNode, builder, externalReferences, className.packageName(), jsonPath);
            }

            traverse(objectToParse, ROOT_OBJECT_NAME, builder, externalReferences, className.packageName(), jsonPath);

            for (String externalRef : externalReferences) {
                Path externalRefPath = jsonPath.getParent().resolve(Paths.get(externalRef.split("#")[0])); //todo more validation here, and/or clean up the jPath/reference stuff.
                String nameOfClass = getNameOfClassFromReference(externalRef);
                String nameOfPackage = getNameOfPackageFromReference(externalRef, jsonPath);
                //TODO: here we assume that all references are objects and result in a class, primitive references should not result in classes , but rather add the field/array to the this builder
                generateClasses(getClassName(nameOfPackage, nameOfClass), externalRefPath, externalRef.split("#")[1], sourceFiles);
            }
            sourceFiles.add(XContentClassBuilder.build(className.packageName(), className.simpleName(), builder));
        }
    }

    private void traverseInlineDefinitions(JsonNode definitionNode, XContentClassBuilder builder, Set<String> externalReferences, String nameOfPackage, Path jsonPath) {
        definitionNode.fields().forEachRemaining(f -> {
            validateObject(f.getValue(), false);
            addField(f.getKey(), getClassName("", formatClassName(f.getKey(), false)), builder, true, false);
            //TODO: support references that are not objects
            traverse(f.getValue(), f.getKey(), addObject(f.getKey(), getClassName("", formatClassName(f.getKey(), false)), builder, false, false, false), externalReferences, nameOfPackage, jsonPath);
        });
    }

    private void traverse(JsonNode node, String key, XContentClassBuilder builder, Set<String> externalReferences, String nameOfPackage, Path jsonPath) {
        JsonNode typeNode = node.get("type");
        validateNotNull(node, typeNode, "type");
        if ("object".equals(typeNode.asText())) {
            validateObject(node, ROOT_OBJECT_NAME.equals(key));
            JsonNode propertiesNode = node.get("properties");
            AtomicBoolean patternPropertiesNode = new AtomicBoolean(false);
            if (propertiesNode == null) {
                propertiesNode = node.get("patternProperties");
                patternPropertiesNode.set(true);
            }
            if (propertiesNode != null) { //support for empty objects by skipping all initialization code
                propertiesNode.fields().forEachRemaining(f -> {
                    //reference to another object
                    JsonNode reference = f.getValue().get("$ref");
                    //not a reference
                    JsonNode nestedTypeNode = f.getValue().get("type");
                    boolean isDeprecrated = f.getValue().get("$deprecated") != null;
                    if (nestedTypeNode != null) {
                        if ("array".equals(nestedTypeNode.asText())) {
                            validateArray(f.getValue());
                            //normal array
                            if (f.getValue().get("items").get("type") != null) {
                                addArray(f.getKey(), getClassName("", f.getValue().get("items").get("type").asText()), builder, false, isDeprecrated);
                            } else { //an array of object references
                                reference = f.getValue().get("items").get("$ref");
                                String refText = reference.asText();
                                if (isExternalReference(refText)) {
                                    externalReferences.add(refText);
                                }
                                addArray(f.getKey(), getClassName(nameOfPackage, getNameOfClassFromReference(refText)), builder, true, isDeprecrated);
                            }
                        } else if ("object".equals(nestedTypeNode.asText())) {
                            validateObject(f.getValue(), ROOT_OBJECT_NAME.equals(key));
                            if (f.getValue().get("patternProperties") != null) { //if this object is a patternProperties
                                JsonNode patternProperties = f.getValue().get("patternProperties");
                                patternProperties.fields().forEachRemaining(p -> {
                                    validatePrimitive(p.getValue());
                                    String nameOfClass = p.getValue().get("type").asText();
                                    if (isReservedClassName(nameOfClass)) {
                                        addMap(f.getKey(), ClassName.bestGuess(formatClassName(nameOfClass, false)), builder, true, isDeprecrated);
                                    } else {
                                        addMap(f.getKey(), getClassName(nameOfPackage, p.getValue().get("type").asText()), builder, true, isDeprecrated);
                                    }
                                });

                            } else if (patternPropertiesNode.get()) { //if the parent is a patternProperties, and this is an object
                                traverse(f.getValue(), f.getKey(), addObject(OBJECT_MAP_ITEM_METHOD_NAME, getClassName("", OBJECT_MAP_ITEM_CLASS_NAME), builder, true, true, isDeprecrated), externalReferences, nameOfPackage, jsonPath);
                            } else {
                                traverse(f.getValue(), f.getKey(), addObject(f.getKey(), getClassName("", formatClassName(f.getKey(), false)), builder, true, false, isDeprecrated), externalReferences, nameOfPackage, jsonPath);
                            }

                        } else {
                            validatePrimitive(f.getValue());
                            String type = nestedTypeNode.asText();
                            addField(f.getKey(), getClassName("", type), builder, false, isDeprecrated);
                        }
                    } else if (reference != null) {
                        String refText = reference.asText();
                        String refPackageName = getNameOfPackageFromReference(refText, jsonPath);
                        if (isExternalReference(refText)) {
                            externalReferences.add(refText); //used to generate the external class
                        }
                        addField(f.getKey(), getClassName(refPackageName, getNameOfClassFromReference(refText)), builder, true, isDeprecrated);

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

    private String getNameOfClassFromReference(String reference) {
        assert reference.contains("#");
        String[] tokens = reference.split("#");
        assert tokens.length == 2;
        assert tokens[0].contains(".json");
        String[] fileParts = tokens[0].split("/");
        String fileName = fileParts[fileParts.length - 1];
        return formatClassName(fileName.replace(".json", ""), true);
    }

    private String getNameOfPackageFromReference(String reference, Path jsonPath) {
        assert reference.contains("#");
        String[] tokens = reference.split("#");
        assert tokens.length == 2;
        Path parent = jsonPath.getParent();
        Path relativePath = parent.resolve(tokens[0]).normalize();
        Path relativePathParent = relativePath.getParent();
        String[] parts = relativePathParent.toString().split("/");
        String packageName = Arrays.stream(parts).dropWhile(p -> "model".equals(p) == false).collect(Collectors.joining("."));
        return packageName.replace("model.", packageRoot + ".");
    }

    //
    private XContentClassBuilder addObject(String field, ClassName className, XContentClassBuilder builder, boolean addToInitialization, boolean isMapOfObjects, boolean isDeprecated) {
        TypeName typeName = isMapOfObjects ? ParameterizedTypeName.get(ClassName.get("java.util", "Map"), ClassName.get(String.class), className) : className;
        //don't add inline reference classes to the initialization blocks
        if (addToInitialization) {
            addInitializationCode(typeName, className.simpleName(), field, builder, true, false, false, isDeprecated);
        }
        XContentClassBuilder child = XContentClassBuilder.newToXContentClassBuilder();
        builder.children.add(new Tuple<>(className, child));
        return child;
    }


    private void addArray(String field, ClassName className, XContentClassBuilder builder, boolean objectReference, boolean isDeprecated) {
        TypeName typeName = ParameterizedTypeName.get(ClassName.get("java.util", "List"), className);
        addInitializationCode(typeName, className.simpleName(), field, builder, objectReference, true, false, isDeprecated);
    }

    private void addField(String field, ClassName className, XContentClassBuilder builder, boolean objectReference, boolean isDeprecated) {
        addInitializationCode(className, className.simpleName(), field, builder, objectReference, false, false, isDeprecated);
    }

    //Map<String, className>
    private void addMap(String field, ClassName className, XContentClassBuilder builder, boolean objectReference, boolean isDeprecated) {
        ParameterizedTypeName typeName = ParameterizedTypeName.get(ClassName.get("java.util", "Map"), ClassName.get(String.class), className);
        addInitializationCode(typeName, className.simpleName(), field, builder, objectReference, false, true, isDeprecated);
    }


    String translateClassName(String nameOfClass) {
        switch (nameOfClass) {
            case "integer":
                return "Long"; //In JSON spec "integer" = non-fractional
            case "number":
                return "Double";  //In JSON spec "number" = fractional
            case "boolean":
                return "Boolean";
            case "string":
                return "String";
            case "null":
                throw new UnsupportedOperationException("null type is not supported");
            default:
                return nameOfClass;
        }
    }

    boolean isReservedClassName(String nameOfClass) {
        switch (nameOfClass.toLowerCase(Locale.ROOT)) {
            case "long":
            case "double":
            case "boolean":
            case "string":
                return true;
            default:
                return false;
        }
    }

    //consumers must call formatClassName before calling this method
    //TODO: don't requires comusers to call formatClassName before calling this method ;)
    ClassName getClassName(String packageName, String nameOfClass) {
        return ClassName.get(packageName, translateClassName(nameOfClass));
    }

    private String formatClassName(String nameOfClass, boolean appendModel) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, nameOfClass) + (appendModel ? postFixName : "");
    }

    //Adds the Constructor and static initialization code to the XContentClassBuilder.
    private void addInitializationCode(TypeName typeName, String simpleClassName, String field, XContentClassBuilder builder, boolean isObject, boolean isArray, boolean isMap, boolean isDeprecated) {
        // ConstructingObjectParser arguments
        if (isArray) {
            builder.lambdas.add(CodeBlock.builder().add("(List<$T>) a[$L]", typeName, builder.parserPosition.incrementAndGet()).build());
        } else {
            builder.lambdas.add(CodeBlock.builder().add("($T) a[$L]", typeName, builder.parserPosition.incrementAndGet()).build());
        }

        //TODO: support required vs. optional
        if (isObject && isArray == false && isMap == false) {
            builder.staticInitializerBuilder.add("PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), " + simpleClassName + ".PARSER, new $T($S));\n", ParseField.class, field);
        } else if (isObject & isArray) {
            builder.staticInitializerBuilder.add("PARSER.declareObjectArray(ConstructingObjectParser.optionalConstructorArg(), " + simpleClassName + ".PARSER, new $T($S));\n", ParseField.class, field);
        } else if (isArray) {
            builder.staticInitializerBuilder.add("PARSER.declare" + simpleClassName + "Array(ConstructingObjectParser.optionalConstructorArg(), new $T($S));\n", ParseField.class, field);
        } else if (isMap) {
            List<TypeName> typeNames = ((ParameterizedTypeName) typeName).typeArguments;
            boolean mapOfStrings = typeNames.stream().allMatch(t -> "String".equals(ClassName.bestGuess(t.toString()).simpleName()));
            if (mapOfStrings) {
                builder.staticInitializerBuilder.add("PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), (p, c) -> p.mapStrings(), new $T($S));\n", ParseField.class, field);

            } else {
                builder.staticInitializerBuilder.add("PARSER.declareObject(ConstructingObjectParser.optionalConstructorArg(), (p, c) -> p.map(), new $T($S));\n", ParseField.class, field);
            }
        } else {
            builder.staticInitializerBuilder.add("PARSER.declare" + simpleClassName + "(ConstructingObjectParser.optionalConstructorArg(), new $T($S));\n", ParseField.class, field);
        }

        builder.constructorBuilder.addParameter(typeName, field).addStatement("this.$N = $N", field, field);
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(typeName, field).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        if (isDeprecated) {
            fieldBuilder.addAnnotation(Deprecated.class);
        }
        builder.fields.add(fieldBuilder.build());

        builder.toXContentFields.add(field);
    }


    /****
     * Validation
     */

    private void validateNotNull(JsonNode parentNode, JsonNode node, String key) {
        if (parentNode == null) {
            throw new NullPointerException("Looking for [" + key + "] in a null JsonNode");
        }
        if (node == null) {
            throw new NullPointerException("Could not find key [" + key + "] in object " + parentNode.toString());
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


}



