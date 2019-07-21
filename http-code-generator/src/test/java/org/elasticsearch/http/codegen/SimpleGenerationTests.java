package org.elasticsearch.http.codegen;

import com.squareup.javapoet.JavaFile;
import org.elasticsearch.test.ESTestCase;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Scanner;

public class SimpleGenerationTests extends ESTestCase {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    public void testFoo() throws IOException {
        byte[] model = toByteArray(ClassLoader.getSystemResourceAsStream("simple.json"));

        print(model);
        HttpApiGenerator generator = new HttpApiGenerator();
        JavaFile generateSource = generator.createSource(new ByteArrayInputStream(model), "co.elastic", "Foo");

        generateSource.writeTo(tempDir.getRoot());
        byte[] generatedFile = toByteArray(new FileInputStream(Files.find(tempDir.getRoot().toPath(), Integer.MAX_VALUE, (p, f) -> f.isRegularFile()).findFirst().orElseThrow().toFile()));

        print(generatedFile);

    }

    private void print(byte[] in) throws IOException {
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
