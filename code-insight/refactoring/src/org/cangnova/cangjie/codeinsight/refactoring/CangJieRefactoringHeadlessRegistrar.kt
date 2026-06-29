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
    /**
     * code-insight 重构扩展声明文件在 classpath 中的固定路径。
     */
    const val PLUGIN_XML_PATH: String = "META-INF/code-insight/cangjie-code-insight-refactoring.xml"

    /**
     * 在 headless application 中注册重构相关 application 级扩展点。
     */
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

    /**
     * 在 headless project 中注册重构相关 project 级服务和扩展点。
     */
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

    /**
     * 若 application 扩展点尚未存在，则按 IntelliJ core 环境规则注册它。
     */
    private fun <T : Any> MockApplication.registerExtensionPointIfMissing(
        name: String,
        extensionClass: Class<T>,
    ) {
        if (extensionArea.getExtensionPointIfRegistered<T>(name) != null) return
        CoreApplicationEnvironment.registerExtensionPoint(extensionArea, name, extensionClass)
    }

    /**
     * 若 project 扩展点尚未存在，则按 IntelliJ core 环境规则注册它。
     */
    private fun <T : Any> MockProject.registerExtensionPointIfMissing(
        name: String,
        extensionClass: Class<T>,
    ) {
        if (extensionArea.getExtensionPointIfRegistered<T>(name) != null) return
        CoreApplicationEnvironment.registerExtensionPoint(extensionArea, name, extensionClass)
    }
}
