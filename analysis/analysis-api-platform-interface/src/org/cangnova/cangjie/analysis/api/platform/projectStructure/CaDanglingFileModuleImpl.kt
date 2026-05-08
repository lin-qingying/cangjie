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
import org.cangnova.cangjie.analysis.api.util.withCaModuleEntry
import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

/**
 * 默认的 dangling file module 平台实现。
 *
 * 这里对位 Kotlin `KaDanglingFileModuleImpl`：模块自身只描述 dangling files 与上下文模块的绑定，
 * 依赖与内容范围直接继承自 [contextModule]。
 */
@CaPlatformInterface
class CaDanglingFileModuleImpl(
    files: List<CjFile>,
    override val contextModule: CaModule,
    override val resolutionMode: CaDanglingFileResolutionMode,
) : CaModuleBase(), CaDanglingFileModule {
    override val isCodeFragment: Boolean = files.any { it is CjCodeFragment }

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

    override val files: List<CjFile>
        get() = validFilesOrNull ?: error("Dangling file module is invalid")

    override val name: String
        get() = files.first().name

    override val languageVersionSettings: LanguageVersionSettings
        get() = (contextModule as? CaSourceModule)?.languageVersionSettings ?: LanguageVersionSettings.DEFAULT

    override val psiRoots: List<CjFile>
        get() = files

    override val project: Project
        get() = contextModule.project

    override val baseContentScope: GlobalSearchScope
        get() {
            val virtualFiles = files.map { it.viewProvider.virtualFile }
            return GlobalSearchScope.filesScope(project, virtualFiles)
        }

    override val directRegularDependencies: List<CaModule>
        get() = contextModule.directRegularDependencies

    override val directDependsOnDependencies: List<CaModule>
        get() = contextModule.directDependsOnDependencies

    override val directFriendDependencies: List<CaModule>
        get() = listOf(contextModule) + contextModule.directFriendDependencies

    override val transitiveDependsOnDependencies: List<CaModule>
        get() = contextModule.transitiveDependsOnDependencies

    override val isValid: Boolean
        get() = validFilesOrNull != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is CaDanglingFileModuleImpl) {
            return fileRefs == other.fileRefs &&
                contextModule == other.contextModule &&
                resolutionMode == other.resolutionMode
        }

        return false
    }

    override fun hashCode(): Int {
        var result = contextModule.hashCode()
        for (fileRef in fileRefs) {
            result = 31 * result + fileRef.hashCode()
        }
        return result
    }

    override fun toString(): String {
        val files = validFilesOrNull
        if (files != null) {
            return files.joinToString { it.name }
        }

        return "Invalid dangling file module"
    }

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
