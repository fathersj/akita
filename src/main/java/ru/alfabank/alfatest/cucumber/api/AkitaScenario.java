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

import com.codeborne.selenide.Selenide;
import lombok.extern.slf4j.Slf4j;
import ru.alfabank.alfatest.cucumber.ScopedVariables;

import java.util.concurrent.TimeUnit;

@Slf4j
public final class AkitaScenario {

    private static AkitaScenario instance = new AkitaScenario();

    private static AkitaEnvironment environment;

    public static AkitaScenario getInstance() {
        return instance;
    }

    public AkitaEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(AkitaEnvironment akitaEnvironment) {
        environment = akitaEnvironment;
    }

    public static void sleep(int seconds) {
        Selenide.sleep(TimeUnit.MILLISECONDS.convert(seconds, TimeUnit.SECONDS));
    }

    public AkitaPage getCurrentPage() {
        return environment.getPages().getCurrentPage();
    }

    /**
     * Set current page of test
     */
    public void setCurrentPage(AkitaPage page) {
        if (page == null) {
            throw new IllegalArgumentException("There's no page " + page);
        }
        environment.getPages().setCurrentPage(page);
    }

    /**
     * Get all pages
     */
    public Pages getPages() {
        return this.getEnvironment().getPages();
    }

    /**
     * Get page by her name
     */
    public AkitaPage getPage(String name) {
        return this.getEnvironment().getPage(name);
    }

    public void write(Object object) {
        this.getEnvironment().write(object);
    }

    /**
     * Get variable from Hashmap of variables
     *
     * @param name - name of vaiable
     */
    public Object getVar(String name) {
        Object obj = this.getEnvironment().getVar(name);
        if (obj == null) {
            throw new IllegalArgumentException("There's no variable with name " + name );
        }
        return obj;
    }

    public Object tryGetVar(String name) {
        return this.getEnvironment().getVar(name);
    }

    public <T extends AkitaPage> T getPage(Class<T> clazz, boolean checkIfElementsAppeared) {
        return Pages.getPage(clazz, checkIfElementsAppeared);
    }

    public <T extends AkitaPage> T getPage(Class<T> clazz) {
        return Pages.getPage(clazz, true);
    }

    public <T extends AkitaPage> T getPage(Class<T> clazz, String name) {
        return this.getEnvironment().getPage(clazz, name);
    }

    public String replaceVariables(String stringToReplaceIn) {
        return this.getEnvironment().replaceVariables(stringToReplaceIn);
    }

    /**
     * Set variable with new value
     */
    public void setVar(String name, Object object) {
        this.getEnvironment().setVar(name, object);
    }

    /**
     * Get all variables
     */
    public ScopedVariables getVars() {
        return this.getEnvironment().getVars();
    }
}