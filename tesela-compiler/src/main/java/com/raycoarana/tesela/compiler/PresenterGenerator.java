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

import com.raycoarana.tesela.Tesela;
import com.raycoarana.tesela.TeselaExecutor;
import com.raycoarana.tesela.annotations.Background;
import com.raycoarana.tesela.annotations.UI;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public class PresenterGenerator {

    private static final String TESELA_EXECUTOR_FIELD_NAME = "mTeselaExecutor";

    private static final ClassName OVERRIDE = ClassName.get(Override.class);
    private static final ClassName BACKGROUND = ClassName.get(Background.class);
    private static final ClassName UI = ClassName.get(UI.class);

    private final PresenterMetaModel presenterMetaModel;
    private final Filer filer;
    private TypeSpec.Builder typeBuilder;

    public static void generate(PresenterMetaModel presenterMetaModel, Filer filer) throws IOException {
        PresenterGenerator presenterGenerator = new PresenterGenerator(presenterMetaModel, filer);
        presenterGenerator.execute();
    }

    private PresenterGenerator(PresenterMetaModel presenterMetaModel, Filer filer) {
        this.presenterMetaModel = presenterMetaModel;
        this.filer = filer;
    }

    private void execute() throws IOException {
        createTypeBuilder();
        createConstructors();
        createBackgroundWrapperMethods();
        createUIWrapperMethods();
        writeToFile();
    }

    private void createTypeBuilder() {
        typeBuilder = TypeSpec.classBuilder(presenterMetaModel.getName())
                .addModifiers(Modifier.PUBLIC)
                .addOriginatingElement(presenterMetaModel.getTypeElement())
                .superclass(TypeName.get(presenterMetaModel.getTypeElement().asType()))
                .addField(TeselaExecutor.class, TESELA_EXECUTOR_FIELD_NAME, Modifier.PRIVATE, Modifier.FINAL);
    }

    private void createConstructors() {
        for (Element element : presenterMetaModel.getTypeElement().getEnclosedElements()) {
            if (element.getKind() == ElementKind.CONSTRUCTOR) {
                createConstructor((ExecutableElement) element);
            }
        }
    }

    private void createConstructor(ExecutableElement element) {
        MethodSpec.Builder constructorBuilder = overriding(element);
        addSuperCallStatement(element, constructorBuilder, false);
        constructorBuilder.addStatement("$L = $T.getExecutor()", TESELA_EXECUTOR_FIELD_NAME, Tesela.class);
        typeBuilder.addMethod(constructorBuilder.build());
    }

    public static MethodSpec.Builder overriding(ExecutableElement method) {
        Set<Modifier> modifiers = method.getModifiers();
        MethodSpec.Builder methodBuilder = MethodSpec.constructorBuilder();

        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            AnnotationSpec annotationSpec = AnnotationSpec.get(mirror);
            methodBuilder.addAnnotation(annotationSpec);
        }

        modifiers = new LinkedHashSet<>(modifiers);
        modifiers.remove(Modifier.ABSTRACT);
        methodBuilder.addModifiers(modifiers);

        for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
            TypeVariable var = (TypeVariable) typeParameterElement.asType();
            methodBuilder.addTypeVariable(TypeVariableName.get(var));
        }

        List<? extends VariableElement> parameters = method.getParameters();
        for (VariableElement parameter : parameters) {
            TypeName type = TypeName.get(parameter.asType());
            String name = parameter.getSimpleName().toString();
            Set<Modifier> parameterModifiers = parameter.getModifiers();
            ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, name)
                    .addModifiers(parameterModifiers.toArray(new Modifier[parameterModifiers.size()]));
            for (AnnotationMirror mirror : parameter.getAnnotationMirrors()) {
                parameterBuilder.addAnnotation(AnnotationSpec.get(mirror));
            }
            methodBuilder.addParameter(parameterBuilder.build());
        }
        methodBuilder.varargs(method.isVarArgs());

        for (TypeMirror thrownType : method.getThrownTypes()) {
            methodBuilder.addException(TypeName.get(thrownType));
        }

        return methodBuilder;
    }

    private void createBackgroundWrapperMethods() {
        for (ExecutableElement element : presenterMetaModel.getBackgroundMethods()) {
            createBackgroundWrapperMethod(element);
        }
    }

    private void createBackgroundWrapperMethod(ExecutableElement element) {
        MethodSpec.Builder builder = overriding(element, BACKGROUND);

        builder.beginControlFlow("if ($L.isMainCurrentThread())", TESELA_EXECUTOR_FIELD_NAME)
                .beginControlFlow("$L.executeInBackground(new Runnable()", TESELA_EXECUTOR_FIELD_NAME)
                .beginControlFlow("public void run()");
        addSuperCallStatement(element, builder, true);
        builder.endControlFlow();

        String tag = element.getAnnotation(Background.class).tag();
        if (Background.NONE.equals(tag)) {
            builder.endControlFlow(")");
        } else {
            builder.endControlFlow(", $S)", tag);
        }
        builder.endControlFlow()
                .beginControlFlow("else");
        addSuperCallStatement(element, builder, false);
        builder.endControlFlow();

        typeBuilder.addMethod(builder.build());
    }

    private void createUIWrapperMethods() {
        for (ExecutableElement element : presenterMetaModel.getUiMethods()) {
            createUiWrapperMethod(element);
        }
    }

    private void createUiWrapperMethod(ExecutableElement element) {
        MethodSpec.Builder builder = overriding(element, UI);

        builder.beginControlFlow("if ($L.isMainCurrentThread())", TESELA_EXECUTOR_FIELD_NAME);
        addSuperCallStatement(element, builder, false);

        builder.endControlFlow()
                .beginControlFlow("else")
                .beginControlFlow("$L.executeInUIThread(new Runnable()", TESELA_EXECUTOR_FIELD_NAME)
                .beginControlFlow("public void run()");

        String viewAttributeName = presenterMetaModel.getViewAttributeName();
        if (viewAttributeName != null) {
            builder.beginControlFlow("if ($L.isValid())", viewAttributeName);
        }

        addSuperCallStatement(element, builder, true);
        builder.endControlFlow()
                .endControlFlow()
                .endControlFlow(")");

        if (viewAttributeName != null) {
            builder.endControlFlow();
        }

        typeBuilder.addMethod(builder.build());
    }

    private void addSuperCallStatement(ExecutableElement element, MethodSpec.Builder builder, boolean includeType) {
        StringBuilder variablesString = new StringBuilder();
        for (VariableElement parameters : element.getParameters()) {
            variablesString.append(parameters.getSimpleName().toString())
                    .append(",");
        }
        if (variablesString.length() > 0) {
            variablesString.delete(variablesString.length() - 1, variablesString.length());
        }
        if (element.getKind() == ElementKind.CONSTRUCTOR) {
            builder.addStatement("super($L)",
                    variablesString);
        } else if (includeType) {
            builder.addStatement("$L.super.$L($L)",
                    presenterMetaModel.getName(),
                    element.getSimpleName().toString(),
                    variablesString);
        } else {
            builder.addStatement("super.$L($L)",
                    element.getSimpleName().toString(),
                    variablesString);
        }
    }

    public static MethodSpec.Builder overriding(ExecutableElement method, ClassName excludeAnnotation) {
        Set<Modifier> modifiers = method.getModifiers();

        String methodName = method.getSimpleName().toString();
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

        methodBuilder.addAnnotation(OVERRIDE);
        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            AnnotationSpec annotationSpec = AnnotationSpec.get(mirror);
            if (annotationSpec.type.equals(OVERRIDE) || annotationSpec.type.equals(excludeAnnotation)) {
                continue;
            }
            methodBuilder.addAnnotation(annotationSpec);
        }

        modifiers = new LinkedHashSet<>(modifiers);
        modifiers.remove(Modifier.ABSTRACT);
        methodBuilder.addModifiers(modifiers);

        for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
            TypeVariable var = (TypeVariable) typeParameterElement.asType();
            methodBuilder.addTypeVariable(TypeVariableName.get(var));
        }

        methodBuilder.returns(TypeName.get(method.getReturnType()));

        List<? extends VariableElement> parameters = method.getParameters();
        for (VariableElement parameter : parameters) {
            TypeName type = TypeName.get(parameter.asType());
            String name = parameter.getSimpleName().toString();
            Set<Modifier> parameterModifiers = parameter.getModifiers();
            ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, name)
                    .addModifiers(parameterModifiers.toArray(new Modifier[parameterModifiers.size()]));
            if (!parameterModifiers.contains(Modifier.FINAL)) {
                parameterBuilder.addModifiers(Modifier.FINAL);
            }
            for (AnnotationMirror mirror : parameter.getAnnotationMirrors()) {
                parameterBuilder.addAnnotation(AnnotationSpec.get(mirror));
            }
            methodBuilder.addParameter(parameterBuilder.build());
        }
        methodBuilder.varargs(method.isVarArgs());

        for (TypeMirror thrownType : method.getThrownTypes()) {
            methodBuilder.addException(TypeName.get(thrownType));
        }

        return methodBuilder;
    }

    private void writeToFile() throws IOException {
        TypeSpec presenterTypeSpec = typeBuilder.build();
        JavaFile javaFile = JavaFile.builder(createPackage(), presenterTypeSpec).build();
        javaFile.writeTo(filer);
    }

    private String createPackage() {
        return presenterMetaModel.getPackageName();
    }

}
