package org.elasticsearch.http.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.collect.Tuple;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
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
        ToXContentClassBuilder xContentClassBuilder = ToXContentClassBuilder.newToXContentClassBuilder();
        traverse(root, ROOT_OBJECT_NAME, ROOT_OBJECT_NAME, xContentClassBuilder);
        return xContentClassBuilder.build(packageName, className);
    }


    private void traverse(JsonNode node, String keyName, String parentKeyName, ToXContentClassBuilder xContentClassBuilder) {
        if (node.isObject()) {
            System.out.println("* Processing node: " + node);

            node.fields().forEachRemaining(e -> traverse(e.getValue(), e.getKey(), keyName,  xContentClassBuilder));

        } else if (node.isArray()) {
            //node.elements().forEachRemaining(n -> traverse("", n, staticInitializerBuilder,  lambdas, constructorBuilder, toXContentMethodBuilder, fields, parserPosition));
        } else if (node.isValueNode()) {
            if ("type".equalsIgnoreCase(keyName)) {
                String value = node.textValue();
                Class<?> clazz = null;
                String cast = "";
                String parserMethodName = "";
                boolean isObject = false;
                boolean isRootObject = false;

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
                        //nested object
                        isObject = true;
                        if (ROOT_OBJECT_NAME.equals(parentKeyName)) {
                            isRootObject = true;
                        } else {
//                            String innerClassName = parentKeyName.substring(0, 1).toUpperCase(Locale.ROOT) + parentKeyName.substring(1).toLowerCase(Locale.ROOT);
//                            cast = "(" + innerClassName + ") ";


                        }
                        break;
                    default:
                        throw new IllegalStateException("Unknown type found [{" + value + "}]");
                }

                //generate the code blocks
//                if (isObject && isRootObject == false) {
//                    lambdas.add(CodeBlock.builder().add(cast + " a[$L]", parserPosition.incrementAndGet()).build());
//                }
                if (isObject == false) {
                    xContentClassBuilder.lambdas.add(CodeBlock.builder().add(cast + " a[$L]", xContentClassBuilder.parserPosition.incrementAndGet()).build());
                    xContentClassBuilder.staticInitializerBuilder.add("PARSER." + parserMethodName + "(ConstructingObjectParser.constructorArg(), new $T($S));\n", ParseField.class, parentKeyName);
                    xContentClassBuilder.constructorBuilder.addParameter(clazz, parentKeyName).addStatement("this.$N = $N", parentKeyName, parentKeyName);
                    xContentClassBuilder.fields.add(FieldSpec.builder(clazz, parentKeyName).addModifiers(Modifier.PUBLIC, Modifier.FINAL).build());
                    xContentClassBuilder.toXContentMethodBuilder.addStatement("builder.field($S," + parentKeyName + ")", parentKeyName);

                }
            }else{

            }

        } else {
            throw new IllegalStateException("Unknown node type, likely invalid JSON");
        }
    }


}
