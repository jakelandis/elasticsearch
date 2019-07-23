package org.elasticsearch.http.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.JavaFile;
import org.elasticsearch.http.ModeledHttpResponse;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
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
import java.util.Map;
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
        LinkedList<ToXContentClassBuilder> xContentClassBuilders = new LinkedList<>();
        List<ObjectModel> objectModels = new ArrayList<>();
        traverse(root, ROOT_OBJECT_NAME, ROOT_OBJECT_NAME, objectModels);
        System.out.println();
        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$");
        objectModels.forEach(m -> {
            System.out.println("******" + m.name + " (parent: " + m.parent + ")");
            m.fields.forEach(f -> {
                System.out.println(f.name + ":" + f.type);
            });
        });
        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$");

        return xContentClassBuilders.getFirst().build(packageName, className);
//        return xContentClassBuilders.getLast().build(packageName, className);
    }


    private void traverse(JsonNode node, String key, String objectParent, List<ObjectModel> objectModels) {

        JsonNode typeNode = node.get("type");
        ObjectModel objectModel = new ObjectModel(key, objectParent);
        objectModels.add(objectModel);
        if ("object".equals(typeNode.asText())) {
            List<Map.Entry<String, JsonNode>> nested = new ArrayList<>();
            JsonNode propertiesNode = node.get("properties");
            propertiesNode.fields().forEachRemaining(f -> {
                JsonNode nestedTypeNode = f.getValue().get("type");
                if ("object".equals(nestedTypeNode.asText()) == false) {
                    objectModel.fields.add(new ObjectModel.Field(f.getKey(), nestedTypeNode.asText()));

                } else {
                    nested.add(f);
                }
            });
            nested.forEach(e -> traverse(e.getValue(), e.getKey(), key, objectModels));
        }
        // node.fields().forEachRemaining(e -> traverse(e.getValue(), e.getKey()));
        //    }
//        } else if (node.isValueNode()) {
//            if ("type".equalsIgnoreCase(currentKey) && "object".equalsIgnoreCase(node.textValue())) {
//
//                System.out.println("* Start processing object " + parentKey + " at depth " + depth.get());
//                //create new builder objects
//                xContentClassBuilders.add(ToXContentClassBuilder.newToXContentClassBuilder());
//
//            } else if ("type".equalsIgnoreCase(currentKey)) {
//                String type = node.textValue();
//                System.out.println("|_Start processing nota object " + parentKey);
//                Class<?> clazz = null;
//                String cast = "";
//                String parserMethodName = "";
//
//
//                switch (type.toLowerCase(Locale.ROOT)) {
//                    case "string":
//                        clazz = String.class;
//                        cast = "(String) ";
//                        parserMethodName = "declareString";
//                        break;
//                    case "integer":
//                        clazz = Long.class; //In JSON spec integer = non-fractional
//                        cast = "(Long) ";
//                        parserMethodName = "declareLong";
//                        break;
//                    case "number":
//                        clazz = Double.class; //In JSON spec number = fractional
//                        cast = "(Double) ";
//                        parserMethodName = "declareDouble";
//                        break;
//                    case "boolean":
//                        clazz = Boolean.class;
//                        cast = "(Boolean) ";
//                        parserMethodName = "declareBoolean";
//                        break;
//                    case "null":
//                        throw new IllegalStateException("`null` type is not supported for code generation");
//
//                    default:
//                        throw new IllegalStateException("Unknown type found [{" + type + "}]");
//                }
//                ToXContentClassBuilder xContentClassBuilder = xContentClassBuilders.peekLast();
//                xContentClassBuilder.lambdas.add(CodeBlock.builder().add(cast + " a[$L]", xContentClassBuilder.parserPosition.incrementAndGet()).build());
//                xContentClassBuilder.staticInitializerBuilder.add("PARSER." + parserMethodName + "(ConstructingObjectParser.constructorArg(), new $T($S));\n", ParseField.class, parentKey);
//                xContentClassBuilder.constructorBuilder.addParameter(clazz, parentKey).addStatement("this.$N = $N", parentKey, parentKey);
//                xContentClassBuilder.fields.add(FieldSpec.builder(clazz, parentKey).addModifiers(Modifier.PUBLIC, Modifier.FINAL).build());
//                xContentClassBuilder.toXContentMethodBuilder.addStatement("builder.field($S," + parentKey + ")", parentKey);
//
//            }
//
//        }
    }


}



