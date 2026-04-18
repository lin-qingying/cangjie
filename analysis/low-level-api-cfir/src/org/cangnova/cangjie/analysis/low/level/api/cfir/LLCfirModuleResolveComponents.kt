/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.PsiModificationTracker
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.DiagnosticsCollector
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.CfirElementBuilder
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirFileBuilder
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.ModuleFileCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.ModuleFileCacheImpl
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructureCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirModuleLazyDeclarationResolver
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLCfirScopeSessionProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider

internal class LLCfirModuleResolveComponents(
    val module: CaModule,
    val globalResolveComponents: LLCfirGlobalResolveComponents,
    val scopeProvider: CfirScopeProvider
) {
    val cache: ModuleFileCache = ModuleFileCacheImpl(this)
    val firFileBuilder: LLCfirFileBuilder = LLCfirFileBuilder(this)
    val firModuleLazyDeclarationResolver = LLCfirModuleLazyDeclarationResolver(this)

    val scopeSessionProvider: LLCfirScopeSessionProvider = LLCfirScopeSessionProvider.create(
        globalResolveComponents.project,
        invalidationTrackers = listOf(
            PsiModificationTracker.MODIFICATION_COUNT,
            ProjectRootModificationTracker.getInstance(globalResolveComponents.project),
        )
    )

    val fileStructureCache: FileStructureCache = FileStructureCache(this)
    val elementsBuilder = CfirElementBuilder(this)
    val diagnosticsCollector = DiagnosticsCollector(fileStructureCache)

    lateinit var session: LLCfirResolvableModuleSession
}