package org.elasticsearch.http.codegen;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedSourceVersion(RELEASE_11)
@SupportedAnnotationTypes("org.elasticsearch.http.ModeledHttpResponse")
public class HttpApiGenerator extends AbstractProcessor {


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "*********** Starting processing here!!&&&");

        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {


        for (Element element : roundEnv.getElementsAnnotatedWith(ModeledHttpResponse.class)) {

            ModeledHttpResponse annotation = element.getAnnotation(ModeledHttpResponse.class);

            String jsonModel = annotation.value();
            String clazz = element.asType().toString();

            try {
                FileObject jsonFile = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", jsonModel);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, jsonFile.toUri().getPath());
                ObjectMapper mapper = new ObjectMapper();


                try (InputStream in = jsonFile.openInputStream()) {
                  JsonNode root = mapper.readTree(in);
                  traverse("root", root);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing " + element.getSimpleName() + ":" + jsonModel + ":" + clazz);

        }


        return true;
    }

    private void traverse(String name, JsonNode node) {

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing : " + name);
        if (node.isObject()) {
            List<Map.Entry<String, JsonNode>> nodes = new ArrayList<>();
            node.fields().forEachRemaining(f -> nodes.add(f));
            nodes.forEach(e -> traverse(e.getKey(), e.getValue()));

        } else if (node.isArray()) {
            List<JsonNode> items = new ArrayList<>();
            node.elements().forEachRemaining(n -> items.add(n));
            items.forEach(n -> traverse("", n));

        } else if (node.isValueNode()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, node.asText());

        } else {
            throw new IllegalStateException("Unknown node type");
        }
    }
}
