/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.analysis.api.util.withCaModuleEntry
import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

/**
 * 默认的 dangling file module 平台实现。
 *
 * 这里对位 Kotlin `KaDanglingFileModuleImpl`：模块自身只描述 dangling files 与上下文模块的绑定，
 * 依赖、目标平台和内容范围都直接继承自 [contextModule]。
 */
@CaPlatformInterface
class CaDanglingFileModuleImpl(
    files: List<CjFile>,
    /**
     * dangling 文件解析时依赖的上下文模块。
     */
    override val contextModule: CaModule,
    /**
     * dangling 文件自身声明与上下文声明的解析优先级。
     */
    override val resolutionMode: CaDanglingFileResolutionMode,
) : CaModuleBase(), CaDanglingFileModule {
    /**
     * 当前 dangling 文件集合是否包含 code fragment。
     */
    override val isCodeFragment: Boolean = files.any { it is CjCodeFragment }

    /**
     * dangling 文件的 smart pointer 列表，用于跨 PSI 生命周期恢复文件。
     */
    @Suppress("DEPRECATION")
    private val fileRefs = files.map { file ->
        SmartPointerManager.getInstance(file.project).createSmartPsiElementPointer(file)
    }

    init {
        require(contextModule != this)

        if (contextModule is CaDanglingFileModule) {
            @OptIn(CaImplementationDetail::class)
            requireWithAttachment(
                isCodeFragment,
                message = { "Dangling file module cannot depend on another dangling file module unless it's a code fragment" },
            ) {
                withCaModuleEntry("contextModule", contextModule)
                withEntryGroup("this") {
                    files.forEachIndexed { index, file -> withPsiEntry("file_$index", file, module = null) }
                    withEntry("resolutionMode", resolutionMode.toString())
                }
            }
        }
    }

    /**
     * 当前仍有效的 dangling 文件列表。
     */
    override val files: List<CjFile>
        get() = validFilesOrNull ?: error("Dangling file module is invalid")

    /**
     * 使用第一个 dangling 文件名作为模块名。
     */
    override val name: String
        get() = files.first().name

    /**
     * dangling 模块继承源码上下文模块的语言版本设置。
     */
    override val languageVersionSettings: LanguageVersionSettings
        get() = (contextModule as? CaSourceModule)?.languageVersionSettings ?: LanguageVersionSettings.DEFAULT

    /**
     * dangling 模块的 PSI 根即 dangling 文件自身。
     */
    override val psiRoots: List<CjFile>
        get() = files

    /**
     * dangling 模块所属项目继承自上下文模块。
     */
    override val project: Project
        get() = contextModule.project

    /**
     * dangling 模块目标平台继承自上下文模块。
     */
    override val targetPlatform: TargetPlatform
        get() = contextModule.targetPlatform

    /**
     * dangling 文件形成的内容搜索范围。
     */
    override val baseContentScope: GlobalSearchScope
        get() {
            val virtualFiles = files.map { it.viewProvider.virtualFile }
            return GlobalSearchScope.filesScope(project, virtualFiles)
        }

    /**
     * 普通依赖继承自上下文模块。
     */
    override val directRegularDependencies: List<CaModule>
        get() = contextModule.directRegularDependencies

    /**
     * depends-on 依赖继承自上下文模块。
     */
    override val directDependsOnDependencies: List<CaModule>
        get() = contextModule.directDependsOnDependencies

    /**
     * friend 依赖包含上下文模块及其 friend 依赖。
     */
    override val directFriendDependencies: List<CaModule>
        get() = listOf(contextModule) + contextModule.directFriendDependencies

    /**
     * 传递 depends-on 依赖继承自上下文模块。
     */
    override val transitiveDependsOnDependencies: List<CaModule>
        get() = contextModule.transitiveDependsOnDependencies

    /**
     * dangling 文件 smart pointer 仍能恢复有效文件时模块有效。
     */
    override val isValid: Boolean
        get() = validFilesOrNull != null

    /**
     * 按文件 smart pointer、上下文模块和解析模式比较 dangling 模块。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is CaDanglingFileModuleImpl) {
            return fileRefs == other.fileRefs &&
                contextModule == other.contextModule &&
                resolutionMode == other.resolutionMode
        }

        return false
    }

    /**
     * 基于上下文模块和文件 smart pointer 计算哈希值。
     */
    override fun hashCode(): Int {
        var result = contextModule.hashCode()
        for (fileRef in fileRefs) {
            result = 31 * result + fileRef.hashCode()
        }
        return result
    }

    /**
     * 返回 dangling 文件名称列表或失效提示。
     */
    override fun toString(): String {
        val files = validFilesOrNull
        if (files != null) {
            return files.joinToString { it.name }
        }

        return "Invalid dangling file module"
    }

    /**
     * 尝试恢复仍然有效的 dangling 文件列表。
     */
    private val validFilesOrNull: List<CjFile>?
        get() {
            val result = ArrayList<CjFile>(fileRefs.size)
            for (fileRef in fileRefs) {
                val file = fileRef.element?.takeIf { it.isValid } ?: return null
                result.add(file)
            }
            return result
        }
}
