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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public class PresentersMap {

    private Map<String, PresenterMetaModel> presenterMetaModelMap = new HashMap<>();
    private Elements elementUtils;
    private Messager messager;

    public void init(Elements elementUtils, Messager messager) {
        this.elementUtils = elementUtils;
        this.messager = messager;
    }

    public PresenterMetaModel getOrCreateFrom(Element element) {
        Element currentElement = element;
        while (currentElement.getKind() != ElementKind.CLASS) {
            currentElement = currentElement.getEnclosingElement();
        }

        TypeElement presenterTypeElement = (TypeElement) currentElement;
        String presenterClassName = presenterTypeElement.getQualifiedName().toString();
        PresenterMetaModel presenterMetaModel = presenterMetaModelMap.get(presenterClassName);
        if (presenterMetaModel == null) {
            presenterMetaModel = new PresenterMetaModel(presenterTypeElement, elementUtils, messager);
            presenterMetaModelMap.put(presenterClassName, presenterMetaModel);
        }
        return presenterMetaModel;
    }

    public Collection<PresenterMetaModel> all() {
        return presenterMetaModelMap.values();
    }

    public void clear() {
        presenterMetaModelMap.clear();
    }
}
