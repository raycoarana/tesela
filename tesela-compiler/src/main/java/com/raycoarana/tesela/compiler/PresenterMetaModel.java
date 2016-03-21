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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

public class PresenterMetaModel {

    private final TypeElement presenterTypeElement;
    private final Elements elementUtils;
    private final Messager messager;
    private final List<ExecutableElement> backgroundMethods = new ArrayList<>();
    private final List<ExecutableElement> uiMethods = new ArrayList<>();
    private VariableElement viewAttribute;

    public PresenterMetaModel(TypeElement presenterTypeElement, Elements elementUtils, Messager messager) {
        this.presenterTypeElement = presenterTypeElement;
        this.elementUtils = elementUtils;
        this.messager = messager;
    }

    public String getPackageName() {
        return elementUtils.getPackageOf(presenterTypeElement).getQualifiedName().toString();
    }

    public Element getTypeElement() {
        return presenterTypeElement;
    }

    public String getName() {
        return "Tesela" + presenterTypeElement.getSimpleName().toString();
    }

    public void addBackgroundMethod(ExecutableElement element) {
        backgroundMethods.add(element);
    }

    public List<ExecutableElement> getBackgroundMethods() {
        return backgroundMethods;
    }

    public void addUIMethod(ExecutableElement element) {
        uiMethods.add(element);
    }

    public List<ExecutableElement> getUiMethods() {
        return uiMethods;
    }

    public void setViewAttribute(VariableElement element) {
        if (viewAttribute != null) {
            throw new IllegalStateException("Can't have more than one @ViewReference attribute");
        }

        viewAttribute = element;
    }

    public String getViewAttributeName() {
        return viewAttribute != null ? viewAttribute.getSimpleName().toString() : null;
    }

    public void validate() {
        validateBackgroundMethodsHasCorrectModifier();
        validateUIMethodsHasCorrectModifier();

        if (viewAttribute.getModifiers().contains(Modifier.PRIVATE)) {
            throw new IllegalStateException("@ViewReference attribute can't be private");
        }

        validateMethodReturnType(getBackgroundMethods());
        validateMethodReturnType(getUiMethods());
    }

    private void validateBackgroundMethodsHasCorrectModifier() {
        validateMethodNotPrivateNotFinalNotAbstract(getBackgroundMethods());
    }

    private void validateUIMethodsHasCorrectModifier() {
        validateMethodNotPrivateNotFinalNotAbstract(getUiMethods());
    }

    private void validateMethodNotPrivateNotFinalNotAbstract(List<ExecutableElement> elements) {
        for (ExecutableElement element : elements) {
            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                printBadModifier(element, "private");
            } else if (element.getModifiers().contains(Modifier.ABSTRACT)) {
                printBadModifier(element, "abstract");
            } else if (element.getModifiers().contains(Modifier.FINAL)) {
                printBadModifier(element, "final");
            }
        }
    }

    private void printBadModifier(ExecutableElement element, String modifier) {
        messager.printMessage(Diagnostic.Kind.ERROR,
                String.format("Method %s in %s can't be %s", element.getSimpleName().toString(), presenterTypeElement.getSimpleName().toString(), modifier));
    }

    private void validateMethodReturnType(List<ExecutableElement> methods) {
        for (ExecutableElement method : methods) {
            if (method.getReturnType().getKind() != TypeKind.NONE) {
                throw new IllegalStateException("Method must not return anything");
            }
        }
    }
}
