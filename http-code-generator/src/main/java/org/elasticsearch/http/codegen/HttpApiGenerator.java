package org.elasticsearch.http.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
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

import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedSourceVersion(RELEASE_11)
@SupportedAnnotationTypes("org.elasticsearch.http.ModeledHttpResponse")
public class HttpApiGenerator extends AbstractProcessor {


    private static final String ROOT_OBJECT_NAME = "__ROOT__";

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
        traverse(root, ROOT_OBJECT_NAME, builder);

        return ToXContentClassBuilder.build(packageName, className, builder);
    }


    private void traverse(JsonNode node, String key, ToXContentClassBuilder builder) {
        JsonNode typeNode = node.get("type");
        if ("object".equals(typeNode.asText())) {
            JsonNode propertiesNode = node.get("properties");
            propertiesNode.fields().forEachRemaining(f -> {
                JsonNode nestedTypeNode = f.getValue().get("type");
                if ("object".equals(nestedTypeNode.asText()) == false) {
                    String type = nestedTypeNode.asText();
                    addPrimitive(f.getKey(), type, builder);
                } else {
                    traverse(f.getValue(), f.getKey(), addObject(f.getKey(), builder));
                }
            });
        }
    }

    private ToXContentClassBuilder addObject(String key, ToXContentClassBuilder builder) {
        ClassName className = ClassName.get("", CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, key));
        addCode(className, key, builder, true);
        ToXContentClassBuilder child = ToXContentClassBuilder.newToXContentClassBuilder();
        builder.children.add(new Tuple<>(className, child));
        return child;
    }

    private void addPrimitive(String field, String type, ToXContentClassBuilder builder) {

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

        addCode(className, field, builder, false);
    }

    private void addCode(ClassName className, String field, ToXContentClassBuilder builder, boolean isObject) {
        builder.lambdas.add(CodeBlock.builder().add("(" + className.simpleName() + ") a[$L]", builder.parserPosition.incrementAndGet()).build());
        if (isObject) {
            builder.staticInitializerBuilder.add("PARSER.declareObject(ConstructingObjectParser.constructorArg(), " + className.simpleName() + ".PARSER, new $T($S));\n", ParseField.class, field);
        } else {
            builder.staticInitializerBuilder.add("PARSER.declare" + className.simpleName() + "(ConstructingObjectParser.constructorArg(), new $T($S));\n", ParseField.class, field);
        }
        builder.constructorBuilder.addParameter(className, field).addStatement("this.$N = $N", field, field);
        builder.fields.add(FieldSpec.builder(className, field).addModifiers(Modifier.PUBLIC, Modifier.FINAL).build());
        builder.toXContentMethodBuilder.addStatement("builder.field($S," + field + ")", field);
    }


}



