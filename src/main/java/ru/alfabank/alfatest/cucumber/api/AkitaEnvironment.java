/**
 * Copyright 2017 Alfa Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.alfabank.alfatest.cucumber.api;

import cucumber.api.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import ru.alfabank.alfatest.cucumber.ScopedVariables;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;

/**
 * Scenario for using variables and pages with annotation "AkitaPage"
 */
@Slf4j
public class AkitaEnvironment {

    private Scenario scenario;

    /**
     * Variables for using in tests
     */
    private ThreadLocal<ScopedVariables> variables = new ThreadLocal<>();

    /**
     * List of pages with annotation AkitaPage
     */
    private Pages pages = new Pages();

    public AkitaEnvironment(Scenario scenario) {
        this.scenario = scenario;
        initPages();
    }

    public AkitaEnvironment() {
        initPages();
    }

    /**
     * Find and add all classes with annotation
     * "AkitaPage" to Pages
     */
    @SuppressWarnings("unchecked")
    private void initPages() {
        getClassesAnnotatedWith(AkitaPage.Name.class)
                .stream()
                .map(it -> {
                    if (AkitaPage.class.isAssignableFrom(it)) {
                        return (Class<? extends AkitaPage>) it;
                    } else {
                        return null;
                    }
                })
                .forEach(addClass -> pages.put(getClassAnnotationValue(addClass), addClass));
    }

    private String getClassAnnotationValue(Class<?> c) {
        return Arrays.stream(c.getAnnotationsByType(AkitaPage.Name.class))
                .findAny()
                .map(AkitaPage.Name::value)
                .orElseThrow(() -> new AssertionError(String.format("There's no annotation in %s class", c.getClass().getName())));
    }

    /**
     * Additional log (level INFO)
     */
    public void write(Object object) {
        scenario.write(String.valueOf(object));
    }

    public ScopedVariables getVars() {
        return getVariables();
    }

    public Object getVar(String name) {
        return getVariables().get(name);
    }

    public void setVar(String name, Object object) {
        getVariables().remove(name);
        getVariables().put(name, object);
    }

    public Pages getPages() {
        return pages;
    }

    public AkitaPage getPage(String name) {
        return pages.get(name);
    }

    public <T extends AkitaPage> T getPage(Class<T> clazz, String name) {
        return pages.get(clazz, name);
    }

    public String replaceVariables(String textToReplaceIn) {
        return getVariables().replaceVariables(textToReplaceIn);
    }

    private ScopedVariables getVariables() {
        if (variables.get() == null) {
            variables.set(new ScopedVariables());
        }
        return variables.get();
    }

    private static Reflections reflection = new Reflections();

    /**
     * Find all classes with annotation
     */
    private Set<Class<?>> getClassesAnnotatedWith(Class<? extends Annotation> annotation) {
        return reflection.getTypesAnnotatedWith(annotation);
    }
}