package org.elasticsearch.http.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedSourceVersion(RELEASE_11)
@SupportedAnnotationTypes("org.elasticsearch.http.ModeledHttpResponse")
public class HttpApiGenerator extends AbstractProcessor {


    List<String> strings = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "*********** Starting processing here");

        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getElementsAnnotatedWith(ModeledHttpResponse.class)) {

            ModeledHttpResponse annotation = element.getAnnotation(ModeledHttpResponse.class);


            //TODO: is it safe to use toString here? will generics get in the way ?
            String clazzOrigin = element.asType().toString();


            //prepend Generated, and append version number
            String simpleClazzName = Arrays.stream(clazzOrigin.split("\\.")).reduce((f, s) -> s).get();

            Pattern pattern = Pattern.compile("(.*)v(\\d+)(.*)");

            String previous = annotation.previous();
            Matcher matcher = pattern.matcher(previous);
            matcher.find();
            String targetSimpleClassName = "Generated" + simpleClazzName + matcher.group(2);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing: " + element.getSimpleName() + " model: " + previous + " from:" + element.asType().toString() + " to: " + targetSimpleClassName);
            read(previous);
            createSource(clazzOrigin.replace("." + simpleClazzName, ""), targetSimpleClassName);
//
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

    private void read(String modelFile) {

        try {
            //read model file
            FileObject jsonFile = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", modelFile);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Reading: " + jsonFile.toUri().getPath());
            ObjectMapper mapper = new ObjectMapper();

            try (InputStream in = jsonFile.openInputStream()) {
                JsonNode root = mapper.readTree(in);
                traverse("root", root);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void createSource(String packageName, String className) {

        try {
            //static parser
            ParameterizedTypeName parameterizedTypes = ParameterizedTypeName.get(
                ClassName.get(ConstructingObjectParser.class),
                ClassName.get(packageName, className),
                ClassName.get(Void.class));
            FieldSpec parserSpec = FieldSpec.builder(parameterizedTypes,
                "PARSER", Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC)
                .build();

            //constructor
            MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
            strings.forEach(s -> constructorBuilder.addParameter(String.class, s).addStatement("this.$N = $N", s, s));
            MethodSpec constructor = constructorBuilder.build();

            //fields
            List<FieldSpec> fields = strings.stream().map(s -> FieldSpec.builder(String.class, s)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build()).collect(Collectors.toList());


            //toXContent
            MethodSpec.Builder toXContentBuilder = MethodSpec.methodBuilder("toXContent")
                .addParameter(XContentBuilder.class, "builder", Modifier.FINAL)
                .addParameter(ToXContent.Params.class, "params", Modifier.FINAL)
                .addAnnotation(Override.class);
            //toXContent contents
            toXContentBuilder
                .addStatement("builder.startObject()");
            strings.forEach(s -> {
                toXContentBuilder.addStatement("builder.field($S," + s + ")", s);
            });

            toXContentBuilder
                .addStatement("builder.endObject()")
                .addStatement("return builder")
                .returns(XContentBuilder.class)
                .build();

            MethodSpec toXContent = toXContentBuilder.build();
            //class
            TypeSpec.Builder clazzBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(parserSpec)
                .addMethod(constructor)
                .addMethod(toXContent);
            fields.forEach(clazzBuilder::addField);
            TypeSpec clazz = clazzBuilder.build();

            JavaFile javaFile = JavaFile.builder(packageName, clazz).build();
            //TODO: actually write file
            javaFile.writeTo(System.out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void traverse(String name, JsonNode node) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing : " + name);
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> traverse(e.getKey(), e.getValue()));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(n -> traverse("", n));
        } else if (node.isValueNode()) {
            String value = node.asText();
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, value);
            if ("string".equals(value)) {
                strings.add(name);
            }

        } else {
            throw new IllegalStateException("Unknown node type");
        }
    }
}
