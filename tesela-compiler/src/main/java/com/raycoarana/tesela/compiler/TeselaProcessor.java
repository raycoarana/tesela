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

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes({
        "com.raycoarana.tesela.annotations.Background",
        "com.raycoarana.tesela.annotations.UI",
        "com.raycoarana.tesela.annotations.View",
        "com.raycoarana.tesela.annotations.ViewReference",
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TeselaProcessor extends AbstractProcessor {

    private static int round = 0;

    private PresentersMap presentersMap = new PresentersMap();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        presentersMap.init(processingEnv.getElementUtils(), processingEnv.getMessager());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement typeElement : annotations) {

            if (typeElement.getSimpleName().toString().equals("View")) {
                processView(roundEnv, typeElement);
            } else {
                processPresenter(roundEnv, typeElement);
            }
        }

        generatePresenters();

        return true;
    }

    private void processView(RoundEnvironment roundEnv, TypeElement typeElement) {
        for (Element element : roundEnv.getElementsAnnotatedWith(typeElement)) {
            WeakViewGenerator.generate((TypeElement) element, processingEnv);
        }
    }

    private void processPresenter(RoundEnvironment roundEnv, TypeElement typeElement) {
        for (Element element : roundEnv.getElementsAnnotatedWith(typeElement)) {
            PresenterMetaModel presenterMetaModel = presentersMap.getOrCreateFrom(element);

            switch (typeElement.getSimpleName().toString()) {
                case "Background":
                    presenterMetaModel.addBackgroundMethod((ExecutableElement) element);
                    break;
                case "UI":
                    presenterMetaModel.addUIMethod((ExecutableElement) element);
                    break;
                case "ViewReference":
                    presenterMetaModel.setViewAttribute((VariableElement) element);
                    break;
            }
        }
    }

    private void generatePresenters() {
        for (PresenterMetaModel presenterMetaModel : presentersMap.all()) {
            try {
                PresenterGenerator.generate(presenterMetaModel, processingEnv.getFiler());
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                throw new RuntimeException("Failed to generate " + presenterMetaModel.getName(), e);
            }
        }
        presentersMap.clear();
    }

}
