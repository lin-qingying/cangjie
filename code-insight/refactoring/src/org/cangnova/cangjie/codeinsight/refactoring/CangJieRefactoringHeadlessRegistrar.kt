/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.lang.refactoring.NamesValidator
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.editor.impl.DocumentWriteAccessGuard
import com.intellij.refactoring.RefactoringHelper
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider
import com.intellij.refactoring.listeners.RefactoringListenerManager
import com.intellij.refactoring.listeners.impl.RefactoringListenerManagerImpl
import com.intellij.refactoring.rename.RenameInputValidator
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.refactoring.suggested.SuggestedRefactoringProvider
import com.intellij.psi.search.ScopeOptimizer

/**
 * Headless 重构扩展注册入口。
 *
 * IDE 插件通过 plugin.xml 让 IntelliJ 平台完成注册；LSP 没有完整插件容器，
 * 需要在自己的 headless application 中显式注册 IntelliJ refactoring 扩展点，
 * 再加载 code-insight 提供的处理器声明。
 */
object CangJieRefactoringHeadlessRegistrar {
    const val PLUGIN_XML_PATH: String = "META-INF/code-insight/cangjie-code-insight-refactoring.xml"

    fun registerExtensionPoints(application: MockApplication) {
        application.registerExtensionPointIfMissing(
            "com.intellij.lang.namesValidator",
            NamesValidator::class.java,
        )
        application.registerExtensionPointIfMissing(
            "com.intellij.lang.refactoringSupport",
            RefactoringSupportProvider::class.java,
        )
        application.registerExtensionPointIfMissing(
            "com.intellij.renameInputValidator",
            RenameInputValidator::class.java,
        )
        application.registerExtensionPointIfMissing(
            "com.intellij.renamePsiElementProcessor",
            RenamePsiElementProcessor::class.java,
        )
        application.registerExtensionPointIfMissing(
            "com.intellij.automaticRenamerFactory",
            AutomaticRenamerFactory::class.java,
        )
        application.registerExtensionPointIfMissing(
            "com.intellij.refactoring.helper",
            RefactoringHelper::class.java,
        )
        application.registerExtensionPointIfMissing(
            "com.intellij.useScopeOptimizer",
            ScopeOptimizer::class.java,
        )
        application.registerExtensionPointIfMissing(
            "com.intellij.documentWriteAccessGuard",
            DocumentWriteAccessGuard::class.java,
        )
    }

    fun registerProjectServices(project: MockProject) {
        project.registerExtensionPointIfMissing(
            "com.intellij.refactoring.elementListenerProvider",
            RefactoringElementListenerProvider::class.java,
        )
        if (project.getService(RefactoringListenerManager::class.java) == null) {
            project.registerService(
                RefactoringListenerManager::class.java,
                RefactoringListenerManagerImpl(project),
            )
        }
        if (project.getService(SuggestedRefactoringProvider::class.java) == null) {
            project.registerService(
                SuggestedRefactoringProvider::class.java,
                CangJieHeadlessSuggestedRefactoringProvider(),
            )
        }
    }

    private fun <T : Any> MockApplication.registerExtensionPointIfMissing(
        name: String,
        extensionClass: Class<T>,
    ) {
        if (extensionArea.getExtensionPointIfRegistered<T>(name) != null) return
        CoreApplicationEnvironment.registerExtensionPoint(extensionArea, name, extensionClass)
    }

    private fun <T : Any> MockProject.registerExtensionPointIfMissing(
        name: String,
        extensionClass: Class<T>,
    ) {
        if (extensionArea.getExtensionPointIfRegistered<T>(name) != null) return
        CoreApplicationEnvironment.registerExtensionPoint(extensionArea, name, extensionClass)
    }
}
