package org.elasticsearch.http.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedSourceVersion(RELEASE_11)
@SupportedAnnotationTypes("org.elasticsearch.http.ModeledHttpResponse")
public class HttpApiGenerator extends AbstractProcessor {


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
        ClassName classToGenerate = ClassName.get(packageName, className);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(model);
        //static initializer
        CodeBlock.Builder staticInitializerBuilder = CodeBlock.builder();
        //constructor
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        //toXContent method
        MethodSpec.Builder toXContentBuilder = MethodSpec.methodBuilder("toXContent")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(XContentBuilder.class, "builder", Modifier.FINAL)
            .addParameter(ToXContent.Params.class, "params", Modifier.FINAL)
            .addAnnotation(Override.class);
        toXContentBuilder.addStatement("builder.startObject()");
        //fields to be added
        List<FieldSpec> fields = new ArrayList<>();
        List<CodeBlock> lambdas = new ArrayList<>();

        traverse("", "", root, staticInitializerBuilder, lambdas, constructorBuilder, toXContentBuilder, fields, new AtomicInteger(0));

        //ConstructingObjectParser<ClassToGenerate, Void>
        ParameterizedTypeName typedConstructingObjectParser = ParameterizedTypeName.get(ClassName.get(ConstructingObjectParser.class), classToGenerate, ClassName.get(Void.class));

        //lambda builder for the static parser
        CodeBlock.Builder lambdaBuilder = CodeBlock.builder();
        lambdaBuilder.add("a -> new $T(", classToGenerate);
        int i = 0;
        for (CodeBlock lambda : lambdas) {
            lambdaBuilder.add("\n").add(lambda);
            if (++i != lambdas.size()) {
                lambdaBuilder.add(",");
            }
        }

        FieldSpec parserSpec = FieldSpec.builder(typedConstructingObjectParser,
            "PARSER", Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC)
            .initializer("new $T<>($T.class.getName(), $L))", ClassName.get(ConstructingObjectParser.class), classToGenerate, lambdaBuilder.build())
            .build();

        MethodSpec toContentMethod = toXContentBuilder
            .addStatement("builder.endObject()")
            .addStatement("return builder")
            .returns(XContentBuilder.class)
            .addException(IOException.class)
            .build();

        //class
        TypeSpec.Builder clazzBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(ToXContentObject.class)
            .addJavadoc("GENERATED CODE - DO NOT MODIFY") //todo add date and by what
            .addStaticBlock(staticInitializerBuilder.build())
            .addField(parserSpec)
            .addMethod(constructorBuilder.build())
            .addMethod(toContentMethod);
        fields.forEach(clazzBuilder::addField);
        TypeSpec clazz = clazzBuilder.build();
        return JavaFile.builder(packageName, clazz).build();
    }

    private void traverse(String name, String parentName, JsonNode node, CodeBlock.Builder staticInitializerBuilder, List<CodeBlock> lamdas, MethodSpec.Builder constructorBuilder, MethodSpec.Builder toXContentBuilder, List<FieldSpec> fields, AtomicInteger parserPosition) {
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> traverse(e.getKey(), name, e.getValue(), staticInitializerBuilder, lamdas, constructorBuilder, toXContentBuilder, fields, parserPosition));
        } else if (node.isArray()) {
            //node.elements().forEachRemaining(n -> traverse("", n, staticInitializerBuilder,  lamdas, constructorBuilder, toXContentBuilder, fields, parserPosition));
        } else if (node.isValueNode()) {
            if ("type".equalsIgnoreCase(name)) {
                String value = node.textValue();
                Class<?> clazz = null;
                String cast = "";
                String parserMethodName = "";
                boolean skip = false;

                switch (value.toLowerCase(Locale.ROOT)) {
                    case "string":
                        clazz = String.class;
                        cast = "(String) ";
                        parserMethodName = "declareString";
                        break;
                    case "integer":
                        clazz = Long.class; //In JSON spec integer = non-fractional
                        cast = "(Long) ";
                        parserMethodName = "declareLong";
                        break;
                    case "number":
                        clazz = Double.class; //In JSON spec number = fractional
                        cast = "(Double) ";
                        parserMethodName = "declareDouble";
                        break;
                    case "boolean":
                        clazz = Boolean.class;
                        cast = "(Boolean) ";
                        parserMethodName = "declareBoolean";
                        break;
                    case "null":
                        throw new IllegalStateException("`null` type is not supported for code generation");
                    case "object":
                        skip = true;
                        break;
                    default:
                        throw new IllegalStateException("Unknown type found [{" + value + "}]");
                }

                //generate the code blocks
                if (skip == false) {
                    lamdas.add(CodeBlock.builder().add(cast + " a[$L]", parserPosition.incrementAndGet()).build());
                    staticInitializerBuilder.add("PARSER." + parserMethodName + "(ConstructingObjectParser.constructorArg(), new $T($S));\n", ParseField.class, parentName);
                    constructorBuilder.addParameter(clazz, parentName).addStatement("this.$N = $N", parentName, parentName);
                    fields.add(FieldSpec.builder(clazz, parentName).addModifiers(Modifier.PUBLIC, Modifier.FINAL).build());
                    toXContentBuilder.addStatement("builder.field($S," + parentName + ")", parentName);
                }
            }

        } else {
            throw new IllegalStateException("Unknown node type, likely invalid JSON");
        }
    }




}
