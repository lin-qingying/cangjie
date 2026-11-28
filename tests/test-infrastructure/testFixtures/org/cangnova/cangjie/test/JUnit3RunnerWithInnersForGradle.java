/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.internal.MethodSorter;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.*;
import org.junit.runner.notification.RunNotifier;

import java.lang.reflect.Method;

import static org.cangnova.cangjie.test.JUnit3RunnerWithInners.isTestMethod;

/**
 * Gradle 已逐类发现嵌套测试，本运行器仅运行当前类，生命周期交给 JUnit3 TestSuite。
 * 外类过滤由 ProjectTestsConvention 在 CLI 和 build-script filters 中补全，不反射旧版 Gradle 字段。
 * 只有嵌套测试的空外类保留为容器，不上报虚假的叶测试事件。
 */
class JUnit3RunnerWithInnersForGradle extends Runner implements Filterable, Sortable {
    private final JUnit38ClassRunner delegateRunner;

    public JUnit3RunnerWithInnersForGradle(Class<?> klass) {
        super();

        Test test = new TestSuite(klass.asSubclass(TestCase.class));
        if (!hasOwnTestMethods(klass)) {
            for (Class<?> declaredClass : klass.getDeclaredClasses()) {
                if (TestCase.class.isAssignableFrom(declaredClass)) {
                    test = new JUnit3RunnerWithInners.FakeEmptyClassTest(klass);
                    break;
                }
            }
        }

        delegateRunner = new JUnit38ClassRunner(test);
    }

    @Override
    public void run(RunNotifier notifier) {
        delegateRunner.run(notifier);
    }

    @Override
    public Description getDescription() {
        return delegateRunner.getDescription();
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        delegateRunner.filter(filter);
    }

    @Override
    public void sort(Sorter sorter) {
        delegateRunner.sort(sorter);
    }

    private static boolean hasOwnTestMethods(Class klass) {
        for (Method each : MethodSorter.getDeclaredMethods(klass)) {
            if (isTestMethod(each)) return true;
        }

        return false;
    }
}
