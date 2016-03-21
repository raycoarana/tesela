/*
    Copyright 2016 Rayco Araña

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.raycoarana.tesela.compiler;

import com.raycoarana.tesela.ViewProxy;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

public class WeakViewGenerator {

    private static final String UI_CONTACT_HOLDER_FIELD_NAME = "mUIContextHolder";
    private static final String WEAK_PREFIX = "Weak";

    private final Elements elementUtils;
    private final TypeElement viewInterfaceElement;
    private final Filer filer;
    private TypeSpec.Builder typeBuilder;

    public static void generate(TypeElement element, ProcessingEnvironment processingEnvironment) {
        WeakViewGenerator generator = new WeakViewGenerator(element,
                processingEnvironment.getFiler(),
                processingEnvironment.getElementUtils());
        try {
            generator.generate();
        } catch (IOException e) {
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            throw new RuntimeException("Failed to generate " + generator.getName(), e);
        }
    }

    public WeakViewGenerator(TypeElement element, Filer filer, Elements elementUtils) {
        this.elementUtils = elementUtils;
        this.viewInterfaceElement = element;
        this.filer = filer;
    }

    private void generate() throws IOException {
        verifyViewIsInterface();
        createTypeBuilder();
        createOfStaticMethod();
        createConstructor();
        createProxyMethods();
        writeToFile();
    }

    private void verifyViewIsInterface() {
        if (viewInterfaceElement.getKind() != ElementKind.INTERFACE) {
            throw new IllegalStateException("Annotation @View is only valid on interfaces, found in " + viewInterfaceElement.getQualifiedName().toString());
        }
    }

    private void createTypeBuilder() {
        typeBuilder = TypeSpec.classBuilder(getName())
                .addModifiers(Modifier.PUBLIC)
                .addOriginatingElement(viewInterfaceElement)
                .addSuperinterface(TypeName.get(viewInterfaceElement.asType()));

        typeBuilder.superclass(ParameterizedTypeName.get(ClassName.get(ViewProxy.class),
                TypeName.get(viewInterfaceElement.asType())));
    }

    private void createOfStaticMethod() {
        ParameterSpec viewParameter = ParameterSpec.builder(TypeName.get(viewInterfaceElement.asType()), "view")
                .build();

        typeBuilder.addMethod(MethodSpec.methodBuilder("of")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(viewParameter)
                .addStatement("return new $L(view)", getName())
                .returns(ClassName.bestGuess(getName()))
                .build());
    }

    private void createConstructor() {
        ParameterSpec viewParameter = ParameterSpec.builder(TypeName.get(viewInterfaceElement.asType()), "view")
                .build();

        typeBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(viewParameter)
                .addStatement("super($L)", "view")
                .build());
    }

    private void createProxyMethods() {
        for (Element element : viewInterfaceElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.METHOD) {
                createProxyMethod((ExecutableElement) element);
            }
        }
    }

    private void createProxyMethod(ExecutableElement element) {
        MethodSpec.Builder builder = MethodSpec.overriding(element);
        StringBuilder statementBuilder = new StringBuilder();
        if (element.getReturnType().getKind() != TypeKind.VOID) {
            statementBuilder.append("return ");
        }
        statementBuilder.append("$L.get().$L(");

        for (VariableElement parameter : element.getParameters()) {
            statementBuilder.append(parameter.getSimpleName().toString());
            statementBuilder.append(",");
        }
        if (element.getParameters().size() > 0) {
            statementBuilder.deleteCharAt(statementBuilder.length() - 1);
        }

        statementBuilder.append(")");
        builder.addStatement(statementBuilder.toString(), UI_CONTACT_HOLDER_FIELD_NAME, element.getSimpleName().toString());

        typeBuilder.addMethod(builder.build());
    }

    private String getName() {
        return WEAK_PREFIX + viewInterfaceElement.getSimpleName().toString();
    }

    private void writeToFile() throws IOException {
        TypeSpec presenterTypeSpec = typeBuilder.build();
        JavaFile javaFile = JavaFile.builder(createPackage(), presenterTypeSpec).build();
        javaFile.writeTo(filer);
    }

    private String createPackage() {
        return elementUtils.getPackageOf(viewInterfaceElement).getQualifiedName().toString();
    }

}
