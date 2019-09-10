package org.elasticsearch.xcontent.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import org.elasticsearch.test.ESTestCase;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

//needs to be run with `-Dtests.security.manager=false` (or fix the security manager config :) )
public class SimpleGenerationTests extends ESTestCase {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    public void testFoo() throws IOException, URISyntaxException {
        String modelJson = "ilm/policy.json";
        byte[] model = toByteArray(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream(modelJson)));
        Path jsonPath = Paths.get(ClassLoader.getSystemResource(modelJson).toURI());

        print(model);
        XContentParserCodeGenerator generator = new XContentParserCodeGenerator();
        Set<JavaFile> sourceFiles = new HashSet<>();
        generator.generateClasses(generator.getClassName("", jsonPath.getFileName().toString().split("\\.")[0]), jsonPath, XContentParserCodeGenerator.ROOT_OBJECT_NAME, sourceFiles, Paths.get(ClassLoader.getSystemResource(".").toURI()));

        for (JavaFile sourceFile : sourceFiles) {
            sourceFile.writeTo(tempDir.getRoot());
            File generatedFile = new File(tempDir.getRoot(), sourceFile.packageName.replaceAll("\\.", "/") + "/" + sourceFile.typeSpec.name + ".java");//Files.find(tempDir.getRoot().toPath(), Integer.MAX_VALUE, (p, f) -> f.isRegularFile()).findFirst().orElseThrow().toFile();
            System.out.println(generatedFile.getAbsolutePath());

            print(generatedFile);
            assertTrue(compile(generatedFile));
        }

    }

    private boolean compile(File generatedFile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        return compiler.run(null, null, null, generatedFile.getAbsolutePath()) == 0;
    }

    private void print(File in) throws IOException {
        print(toByteArray(new FileInputStream(in)));
    }

    private void print(byte[] in) {
        Scanner s = new Scanner(new ByteArrayInputStream(in)).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        System.out.println(result);
    }

    private byte[] toByteArray(InputStream in) throws IOException {
        assert in != null;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        in.transferTo(bytes);
        return bytes.toByteArray();
    }

}
