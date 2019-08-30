package org.elasticsearch.http.codegen;

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
import org.elasticsearch.http.ModeledHttpResponse;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedSourceVersion(RELEASE_11)
@SupportedAnnotationTypes("org.elasticsearch.http.ModeledHttpResponse")
public class HttpApiGenerator<a> extends AbstractProcessor {


    private static final String ROOT_OBJECT_NAME = "__ROOT__";

    private static Set<String> VALID_OBJECT_KEYS = Set.of("description", "type", "properties");
    private static Set<String> VALID_ROOT_OBJECT_KEYS = Stream.concat(VALID_OBJECT_KEYS.stream(), Stream.of("$id", "$schema", "definitions")).collect(Collectors.toSet());
    private static Set VALID_ARRAY_KEYS = Set.of("description", "type", "items");
    private static Set VALID_ARRAY_ITEMS_KEYS = Set.of("description", "type", "$ref");
    //TODO more validation

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "*********** Starting processing here");

        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getElementsAnnotatedWith(ModeledHttpResponse.class)) {

            try {
                ModeledHttpResponse annotation = element.getAnnotation(ModeledHttpResponse.class);


                //TODO: is it safe to use toString here? will generics get in the way ?
                String clazzOrigin = element.asType().toString();


                //prepend Generated, and append version number
                String simpleClazzName = Arrays.stream(clazzOrigin.split("\\.")).reduce((f, s) -> s).get();

                Pattern pattern = Pattern.compile("(.*)v(\\d+)(.*)");

                String previous = annotation.previous();
                Matcher matcher = pattern.matcher(previous);
                matcher.find();
                String className = "Generated" + simpleClazzName + matcher.group(2);
                String packageName = clazzOrigin.replace("." + simpleClazzName, "");
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing: " + element.getSimpleName() + " model: " + previous + " from:" + element.asType().toString() + " to: " + className);
                //read model file
                FileObject jsonFile = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", previous);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Reading: " + jsonFile.toUri().getPath());
                try (InputStream in = jsonFile.openInputStream()) {
                    JavaFile javaFile = createSource(in, packageName, className);
                    //write source
                    javaFile.writeTo(processingEnv.getFiler());
                    javaFile.writeTo(System.out);

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
// only handles the super simple case of strings..current has object, so no go yet...
//            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "*********************************************");
//
//            String current = annotation.current();
//            matcher = pattern.matcher(current);
//            matcher.find();
//            targetClassName = baseTargetClassName + matcher.group(2);
//            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing: " + element.getSimpleName() + " model: " + previous + " from:" + element.asType().toString() + " to: " + targetClassName);
//            read(current);


        }

        return true;
    }


    public JavaFile createSource(InputStream model, String packageName, String className) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(model);
        ToXContentClassBuilder builder = ToXContentClassBuilder.newToXContentClassBuilder();
        JsonNode definitionNode = root.get("definitions");
        if (definitionNode != null) {
            traverseDefinitions(definitionNode, builder);
        }
        traverse(root, ROOT_OBJECT_NAME, builder);

        return ToXContentClassBuilder.build(packageName, className, builder);
    }

    private void traverseDefinitions(JsonNode definitionNode, ToXContentClassBuilder builder) {
        //todo: support in file definitions

        definitionNode.fields().forEachRemaining(f -> {
            validateObject(f.getValue(), false);
            traverse(f.getValue(), f.getKey(), addObject(f.getKey(), builder, false));
        });

    }

    private void traverse(JsonNode node, String key, ToXContentClassBuilder builder) {
        JsonNode typeNode = node.get("type");
        if ("object".equals(typeNode.asText())) {
            validateObject(node, ROOT_OBJECT_NAME.equals(key));
            JsonNode propertiesNode = node.get("properties");
            propertiesNode.fields().forEachRemaining(f -> {
                JsonNode nestedTypeNode = f.getValue().get("type");
                if ("array".equals(nestedTypeNode.asText())) {
                    validateArray(f.getValue());

                    //primitive array
                    if (f.getValue().get("items").get("type") != null) {
                        addArray(f.getKey(), f.getValue().get("items").get("type").asText(), builder);
                    } else { //an array of objects
                        //add the code for the array references without traversing
                        //f.getValue().get("items").get("$ref").asText();
//                        assert reference.contains("#");
//                        String[] tokens = reference.split("#");
//                        assert tokens.length == 2;
//                        String file = tokens[0];
//                        String jsonPath = tokens[1];
//                        String referencedObjectName = jsonPath.substring(jsonPath.lastIndexOf("/") + 1);
                    }
                    //todo: handle an array of objects
                } else if ("object".equals(nestedTypeNode.asText())) {
                    validateObject(f.getValue(), ROOT_OBJECT_NAME.equals(key));
                    traverse(f.getValue(), f.getKey(), addObject(f.getKey(), builder, true));
                } else {
                    //todo validate primitive key name with regex
                    String type = nestedTypeNode.asText();
                    addPrimitive(f.getKey(), type, builder);
                }
            });
        }
    }


    private void validateObject(JsonNode node, boolean isRoot) {
        assert node.isObject();
        node.deepCopy().fieldNames().forEachRemaining(name -> {
            Set validObjectKeys = VALID_OBJECT_KEYS;
            if (isRoot) {
                validObjectKeys = VALID_ROOT_OBJECT_KEYS;
            }
            if (validObjectKeys.contains(name) == false) {
                throw new IllegalStateException("Found unsupported key name [" + name + "] in object " + node.toString());
            }
        });
    }

    private void validateArray(JsonNode node) {
        assert node.isObject(); //we are validating the node object that contains the "type" : "array"
        node.deepCopy().fieldNames().forEachRemaining(name -> {

            if (VALID_ARRAY_KEYS.contains(name) == false) {
                throw new IllegalStateException("Found unsupported key name [" + name + "] in object " + node.toString());
            }
            if ((node.get("items").get("type") == null && node.get("items").get("$ref") == null)) {
                throw new IllegalStateException("Could not find primitive type or object $ref for array [" + node.toString() + "]");
            }
            node.get("items").fieldNames().forEachRemaining(i -> {
                if (VALID_ARRAY_ITEMS_KEYS.contains(i) == false) {
                    throw new IllegalStateException("Found unsupported array item name [" + i + "] in object " + node.toString());
                }
            });
        });
    }

    private ToXContentClassBuilder addObject(String key, ToXContentClassBuilder builder, boolean addToInitialization) {
        ClassName className = ClassName.get("", CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, key));
        //don't add reference classes to the initialization blocks
        if(addToInitialization) {
            addInitializationCode(className, key, builder, true, false);
        }
        ToXContentClassBuilder child = ToXContentClassBuilder.newToXContentClassBuilder();
        builder.children.add(new Tuple<>(className, child));
        return child;
    }

    private void addArray(String field, String type, ToXContentClassBuilder builder) {
        ClassName genericType = "object".equals(type) ? ClassName.get(Object.class) : getClassName(type);
        addInitializationCode(genericType, field, builder, false, true);
    }

    private void addPrimitive(String field, String type, ToXContentClassBuilder builder) {
        addInitializationCode(getClassName(type), field, builder, false, false);
    }

    private ClassName getClassName(String type) {
        ClassName className = null;

        switch (type.toLowerCase(Locale.ROOT)) {
            case "string":
                className = ClassName.get(String.class);
                break;
            case "integer":
                className = ClassName.get(Long.class); //In JSON spec "integer" = non-fractional
                break;
            case "number":
                className = ClassName.get(Double.class); //In JSON spec "number" = fractional
                break;
            case "boolean":
                className = ClassName.get(Boolean.class);
                break;
            case "null":
                throw new IllegalStateException("`null` type is not supported for code generation");

            default:
                throw new IllegalStateException("Unknown type found [{" + type + "}]");
        }
        return className;

    }

    //Adds the Constructor and static initialization code to the ToXContentClassBuilder.
    private void addInitializationCode(ClassName className, String field, ToXContentClassBuilder builder, boolean isObject, boolean isArray) {
        // ConstructingObjectParser arguments
        if (isArray) {
            builder.lambdas.add(CodeBlock.builder().add("(List<" + className.simpleName() + ">) a[$L]", builder.parserPosition.incrementAndGet()).build());
        } else {
            builder.lambdas.add(CodeBlock.builder().add("(" + className.simpleName() + ") a[$L]", builder.parserPosition.incrementAndGet()).build());

        }
        // PARSER.declare
        if (isObject) {
            builder.staticInitializerBuilder.add("PARSER.declareObject(ConstructingObjectParser.constructorArg(), " + className.simpleName() + ".PARSER, new $T($S));\n", ParseField.class, field);
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



