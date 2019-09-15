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
import org.elasticsearch.common.xcontent.GenerateXContentModel;
import org.elasticsearch.common.xcontent.GenerateXContentModels;

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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.lang.model.SourceVersion.RELEASE_11;


@SupportedSourceVersion(RELEASE_11)
@SupportedAnnotationTypes({"org.elasticsearch.common.xcontent.GenerateXContentModel", "org.elasticsearch.common.xcontent.GenerateXContentModels"})
@SupportedOptions("spec.root")
public class XContentModelCodeGenerator extends AbstractProcessor {

    static final String ROOT_OBJECT_NAME = ".";


    private static Set<String> VALID_OBJECT_KEYS = Set.of("description", "type", "properties");
    private static Set<String> VALID_PRIMITIVE_KEYS = Set.of("description", "type", "$ref");
    private static Set<String> VALID_ROOT_OBJECT_KEYS = Stream.concat(VALID_OBJECT_KEYS.stream(), Stream.of("$id", "$schema", "definitions")).collect(Collectors.toSet());
    private static Set<String> VALID_ARRAY_KEYS = Set.of("description", "type", "items");
    private static Set<String> VALID_ARRAY_ITEMS_KEYS = Set.of("description", "type", "$ref");
    //TODO more validation

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "*********** Starting processing here" + processingEnv.getOptions().get("spec.root"));

        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "*********************************************3");

        for (Element element : roundEnv.getElementsAnnotatedWithAny(Set.of(GenerateXContentModel.class, GenerateXContentModels.class))) {
            try {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, element.getSimpleName());
                GenerateXContentModels generateXContentModels = element.getAnnotation(GenerateXContentModels.class);
                List<GenerateXContentModel> models = new ArrayList<>();
                if (generateXContentModels != null) {
                    models.addAll(Arrays.asList(generateXContentModels.value()));
                } else {
                    models.add(element.getAnnotation(GenerateXContentModel.class));
                }

                for (GenerateXContentModel model : models) {
                    ClassName originalClassName = ClassName.bestGuess(element.asType().toString());

                    Path apiRootPath = new File(processingEnv.getOptions().get("spec.root")).toPath();
                    Path jsonPath = apiRootPath.resolve(model.model());
                    String nameOfClass = model.className();
                    String nameOfPackage = model.packageName();

                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating somehting something..." + originalClassName + " from " + jsonPath.toAbsolutePath().toString());
                    ClassName className = ClassName.get(nameOfPackage, nameOfClass);
                    HashSet<JavaFile> sourceFiles = new HashSet<>();
                    generateClasses(className, jsonPath, ROOT_OBJECT_NAME, sourceFiles);
                    for (JavaFile sourceFile : sourceFiles) {
                        try (Writer writer = processingEnv.getFiler().createSourceFile(sourceFile.packageName + "." + sourceFile.typeSpec.name).openWriter()) {
                            sourceFile.writeTo(writer);
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
        try (InputStream in = Files.newInputStream(jsonPath)) {
            JsonNode objectToParse = findObjectToParse(in, jPath);
            XContentClassBuilder builder = XContentClassBuilder.newToXContentClassBuilder();

            Set<String> externalReferences = new HashSet<>();
            //handle any inline definitions that will end up as inner classes
            JsonNode definitionNode = objectToParse.get("definitions");
            if (definitionNode != null) {
                traverseInlineDefinitions(definitionNode, builder, externalReferences, className.packageName());
            }

            traverse(objectToParse, ROOT_OBJECT_NAME, builder, externalReferences, className.packageName());

            for (String externalRef : externalReferences) {
                Path externalRefPath = jsonPath.getParent().resolve(Paths.get(externalRef.split("#")[0])); //todo more validation here?
                String nameOfClass = getClassNameFromReference(externalRef);
                //TODO: here we assume that all references are objects and result in a class, primitive references should not result in classes , but rather add the field/array to the this builder
                generateClasses(getClassName(className.packageName(), nameOfClass), externalRefPath, externalRef.split("#")[1], sourceFiles);
            }

            sourceFiles.add(XContentClassBuilder.build(className.packageName(), className.simpleName(), builder));
        }
    }

    private void traverseInlineDefinitions(JsonNode definitionNode, XContentClassBuilder builder, Set<String> externalReferences, String nameOfPackage) {
        definitionNode.fields().forEachRemaining(f -> {
            validateObject(f.getValue(), false);
            addField(f.getKey(), getClassName("", f.getKey()), builder, true);
            //TODO: support references that are not objects
            traverse(f.getValue(), f.getKey(), addObject(f.getKey(), getClassName("", f.getKey()), builder, false), externalReferences, nameOfPackage);
        });
    }

    private void traverse(JsonNode node, String key, XContentClassBuilder builder, Set<String> externalReferences, String nameOfPackage) {
        JsonNode typeNode = node.get("type");
        validateNotNull(node, typeNode, "type");
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
                                }
                                addArray(f.getKey(), getClassName(nameOfPackage, getClassNameFromReference(refText)), builder, true);

                            }
                        } else if ("object".equals(nestedTypeNode.asText())) {
                            validateObject(f.getValue(), ROOT_OBJECT_NAME.equals(key));

                            traverse(f.getValue(), f.getKey(), addObject(f.getKey(), getClassName("", f.getKey()), builder, true), externalReferences, nameOfPackage);
                        } else {

                            validatePrimitive(f.getValue());
                            String type = nestedTypeNode.asText();
                            addField(f.getKey(), getClassName("", type), builder, false);
                        }
                    } else if (reference != null) {
                        String refText = reference.asText();
                        if (isExternalReference(refText)) {
                            externalReferences.add(refText); //used to generate the external class
                        }
                        addField(f.getKey(), getClassName(nameOfPackage, getClassNameFromReference(refText)), builder, true);

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

    private String getClassNameFromReference(String reference) {
        assert reference.contains("#");
        String[] tokens = reference.split("#");
        assert tokens.length == 2;
        return formatClassName(tokens[1].substring(tokens[1].lastIndexOf("/") + 1));
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



