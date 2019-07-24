package org.elasticsearch.http.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ToXContentClassBuilder {

    final CodeBlock.Builder staticInitializerBuilder;
    final List<CodeBlock> lambdas;
    final MethodSpec.Builder constructorBuilder;
    final MethodSpec.Builder toXContentMethodBuilder;
    final List<FieldSpec> fields;
    final AtomicInteger parserPosition;
    final List<Tuple<ClassName, ToXContentClassBuilder>> children;

    static ToXContentClassBuilder newToXContentClassBuilder() {
        //static initializer
        CodeBlock.Builder staticInitializerBuilder = CodeBlock.builder();
        //constructor
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        //toXContent method
        MethodSpec.Builder toXContentMethodBuilder = MethodSpec.methodBuilder("toXContent")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(XContentBuilder.class, "builder", Modifier.FINAL)
            .addParameter(ToXContent.Params.class, "params", Modifier.FINAL)
            .addAnnotation(Override.class);
        toXContentMethodBuilder.addStatement("builder.startObject()");
        //fields to be added
        List<FieldSpec> fields = new ArrayList<>();
        List<CodeBlock> lambdas = new ArrayList<>();
        return new ToXContentClassBuilder(staticInitializerBuilder, lambdas, constructorBuilder, toXContentMethodBuilder, fields, new AtomicInteger(0));
    }

    private ToXContentClassBuilder(CodeBlock.Builder staticInitializerBuilder, List<CodeBlock> lambdas, MethodSpec.Builder constructorBuilder, MethodSpec.Builder toXContentMethodBuilder, List<FieldSpec> fields, AtomicInteger parserPosition) {
        this.staticInitializerBuilder = staticInitializerBuilder;
        this.lambdas = lambdas;
        this.constructorBuilder = constructorBuilder;
        this.toXContentMethodBuilder = toXContentMethodBuilder;
        this.fields = fields;
        this.parserPosition = parserPosition;
        this.children = new ArrayList<>();
    }

    static JavaFile build(String packageName, String targetClassName, ToXContentClassBuilder builder) {
        ClassName className = ClassName.get(packageName, targetClassName);

        // .. new ConstructingObjectParser<> ..
        FieldSpec parserField = createConstructingObjectParser(builder, className);

        //public XContentBuilder toXContent
        MethodSpec toContentMethod = createToXContent(builder);

        //inner classes
        List<TypeSpec> innerClasses = new ArrayList<>();
        addChildren(builder, innerClasses);

        //outer class
        TypeSpec.Builder clazzBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(ToXContentObject.class)
            .addJavadoc("GENERATED CODE - DO NOT MODIFY") //todo add date and by what
            .addStaticBlock(builder.staticInitializerBuilder.build())
            .addField(parserField)
            .addMethod(builder.constructorBuilder.build())
            .addMethod(toContentMethod)
            .addTypes(innerClasses);
        builder.fields.forEach(clazzBuilder::addField);
        TypeSpec clazz = clazzBuilder.build();
        return JavaFile.builder(packageName, clazz).build();
    }

    static FieldSpec createConstructingObjectParser(ToXContentClassBuilder builder, ClassName className) {

        //ConstructingObjectParser<ClassToGenerate, Void>
        ParameterizedTypeName typedConstructingObjectParser = ParameterizedTypeName.get(ClassName.get(ConstructingObjectParser.class), className, ClassName.get(Void.class));

        //lambda builder for the static parser
        CodeBlock.Builder lambdaBuilder = CodeBlock.builder();
        lambdaBuilder.add("a -> new $T(", className);
        int i = 0;
        for (CodeBlock lambda : builder.lambdas) {
            lambdaBuilder.add("\n").add(lambda);
            if (++i != builder.lambdas.size()) {
                lambdaBuilder.add(",");
            }
        }

        return FieldSpec.builder(typedConstructingObjectParser,
            "PARSER", Modifier.STATIC, Modifier.FINAL, Modifier.PUBLIC)
            .initializer("new $T<>($T.class.getName(), $L))", ClassName.get(ConstructingObjectParser.class), className, lambdaBuilder.build())
            .build();
    }

    static MethodSpec createToXContent(ToXContentClassBuilder builder) {
        return builder.toXContentMethodBuilder
            .addStatement("builder.endObject()")
            .addStatement("return builder")
            .returns(XContentBuilder.class)
            .addException(IOException.class)
            .build();
    }

    static TypeSpec createInnerClass(ToXContentClassBuilder builder, ClassName className, FieldSpec parserField, MethodSpec toContentMethod){
        TypeSpec.Builder clazzBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addSuperinterface(ToXContentObject.class)
            .addStaticBlock(builder.staticInitializerBuilder.build())
            .addField(parserField)
            .addMethod(builder.constructorBuilder.build())
            .addMethod(toContentMethod);
        builder.fields.forEach(clazzBuilder::addField);
        return clazzBuilder.build();
    }

    static void addChildren(ToXContentClassBuilder builder, List<TypeSpec> innerClasses){
        List<Tuple<ClassName, ToXContentClassBuilder>> children = builder.children;
        for( Tuple<ClassName, ToXContentClassBuilder> child : children){
            FieldSpec parserField = createConstructingObjectParser(child.v2(), child.v1());
            MethodSpec toContentMethod = createToXContent(child.v2());
            innerClasses.add(createInnerClass(child.v2(), child.v1(), parserField, toContentMethod));
            if(child.v2().children.size() > 0){
                addChildren(child.v2(), innerClasses);
            }
        }
    }
}
