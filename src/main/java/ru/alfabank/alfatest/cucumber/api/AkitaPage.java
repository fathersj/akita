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

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static ru.alfabank.tests.core.helpers.PropertyLoader.loadProperty;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsContainer;
import com.codeborne.selenide.SelenideElement;
import lombok.extern.slf4j.Slf4j;
import ru.alfabank.alfatest.cucumber.utils.Reflection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

/**
 * Annotation for PageObject
 */
@Slf4j
public abstract class AkitaPage extends ElementsContainer {

    /**
     * Timeout for waiting elements
     */
    private static final String WAITING_APPEAR_TIMEOUT_IN_MILLISECONDS = "5000";

    /**
     * Get element by his name
     */
    public SelenideElement getElement(String elementName) {
        return (SelenideElement) java.util.Optional.ofNullable(namedElements.get(elementName))
                .orElseThrow(() -> new IllegalArgumentException(String.format("There's no %s element on the current page %s", elementName, this.getClass().getName())));
    }

    /**
     * Get List of elements by his name
     */
    @SuppressWarnings("unchecked")
    public List<SelenideElement> getElementsList(String listName) {
        Object value = namedElements.get(listName);
        if (!(value instanceof List)) {
            throw new IllegalArgumentException(String.format("There's no %s on the current page %s", listName, this.getClass().getName()));
        }
        Stream<Object> s = ((List) value).stream();
        return s.map(AkitaPage::castToSelenideElement).collect(toList());
    }

    /**
     * Get text of element by his name
     */
    public String getAnyElementText(String elementName) {
        SelenideElement element = getElement(elementName);
        if (element.getTagName().equals("input")) {
            return element.getValue();
        } else {
            return element.innerText();
        }
    }

    /**
     * Get all text of element by his name
     */
    public List<String> getAnyElementsListTexts(String listName) {
        List<SelenideElement> elementsList = getElementsList(listName);
        return elementsList.stream()
                .map(element -> element.getTagName().equals("input")
                        ? element.getValue()
                        : element.innerText()
                )
                .collect(toList());
    }

    /**
     * Annotation Name for using elements by their names in features
     */
    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Name {
        String value();
    }

    /**
     * Annotation for optional elements on the page
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Optional {
    }

    /**
     * Get all element with annotation @Name
     */
    public List<SelenideElement> getPrimaryElements() {
        if (primaryElements == null) {
            primaryElements = readWithWrappedElements();
        }
        return new ArrayList<>(primaryElements);
    }

    /**
     * Check that all non-optional elements are on the current page
     */
    public final AkitaPage appeared() {
        String timeout = loadProperty("waitingAppearTimeout", WAITING_APPEAR_TIMEOUT_IN_MILLISECONDS);
        getPrimaryElements().parallelStream().forEach(elem ->
                elem.waitUntil(Condition.appear, Integer.valueOf(timeout)));
        return this;
    }

    /**
     * Check that all non-optional elements aren't on the current page
     */
    public final AkitaPage disappeared() {
        String timeout = loadProperty("waitingAppearTimeout", WAITING_APPEAR_TIMEOUT_IN_MILLISECONDS);
        getPrimaryElements().parallelStream().forEach(elem ->
                elem.waitWhile(Condition.exist, Integer.valueOf(timeout)));
        return this;
    }

    /**
     * Waiting new condition for elements with timeout
     *
     * @param condition Selenide.Condition
     * @param timeout   time of waiting
     * @param elements  elements
     */
    public void waitElementsUntil(Condition condition, int timeout, SelenideElement... elements) {
        Spectators.waitElementsUntil(condition, timeout, elements);
    }

    public void waitElementsUntil(Condition condition, int timeout, List<SelenideElement> elements) {
        Spectators.waitElementsUntil(condition, timeout, elements);
    }

    /**
     * Waiting new condition for elements by their names, with timeout
     *
     * @param elementNames names of elements
     */
    public void waitElementsUntil(Condition condition, int timeout, String... elementNames) {
        List<SelenideElement> elements = Arrays.stream(elementNames)
                .map(name -> namedElements.get(name))
                .flatMap(v -> v instanceof List ? ((List<?>) v).stream() : Stream.of(v))
                .map(AkitaPage::castToSelenideElement)
                .filter(Objects::nonNull)
                .collect(toList());
        Spectators.waitElementsUntil(condition, timeout, elements);
    }

    /**
     * Find element by his name in the list of elements
     */
    public static SelenideElement getElementFromListByName(List<SelenideElement> listElements, String nameOfElement) {
        List<String> names = new ArrayList<>();
        for (SelenideElement element : listElements) {
            names.add(element.getText());
        }
        return listElements.get(names.indexOf(nameOfElement));
    }

    /**
     * Cast element to SelenideElement
     */
    private static SelenideElement castToSelenideElement(Object object) {
        if (object instanceof SelenideElement) {
            return (SelenideElement) object;
        }
        return null;
    }

    /**
     * Map of all elements on the page
     */
    private Map<String, Object> namedElements;

    /**
     * All non-optional elements
     */
    private List<SelenideElement> primaryElements;

    @Override
    public void setSelf(SelenideElement self) {
        super.setSelf(self);
        initialize();
    }

    AkitaPage initialize() {
        namedElements = readNamedElements();
        primaryElements = readWithWrappedElements();
        return this;
    }

    /**
     * Find and initialize all elements in the page
     */
    private Map<String, Object> readNamedElements() {
        checkNamedAnnotations();
        return Arrays.stream(getClass().getDeclaredFields())
                .filter(f -> f.getDeclaredAnnotation(Name.class) != null)
                .peek(f -> {
                    if (!SelenideElement.class.isAssignableFrom(f.getType()) && !List.class.isAssignableFrom(f.getType()))
                        throw new IllegalStateException(
                                format("Field %s with @Name must has type SelenideElement or List<SelenideElement>, but has %s", f.getName(), f.getType()));
                })
                .collect(toMap(f -> f.getDeclaredAnnotation(Name.class).value(), this::extractFieldValueViaReflection));
    }

    /**
     * Search all elements with annotation Name
     */
    private void checkNamedAnnotations() {
        List<String> list = Arrays.stream(getClass().getDeclaredFields())
                .filter(f -> f.getDeclaredAnnotation(Name.class) != null)
                .map(f -> f.getDeclaredAnnotation(Name.class).value())
                .collect(toList());
        if (list.size() != new HashSet<>(list).size()) {
            throw new IllegalStateException("More then 1 element with the same name " + this.getClass().getName());
        }
    }

    /**
     * Find and initialize all non-optional elements in the page
     */
    private List<SelenideElement> readWithWrappedElements() {
        return Arrays.stream(getClass().getDeclaredFields())
                .filter(f -> f.getDeclaredAnnotation(Optional.class) == null)
                .map(this::extractFieldValueViaReflection)
                .flatMap(v -> v instanceof List ? ((List<?>) v).stream() : Stream.of(v))
                .map(AkitaPage::castToSelenideElement)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private Object extractFieldValueViaReflection(Field field) {
        return Reflection.extractFieldValue(field, this);
    }
}