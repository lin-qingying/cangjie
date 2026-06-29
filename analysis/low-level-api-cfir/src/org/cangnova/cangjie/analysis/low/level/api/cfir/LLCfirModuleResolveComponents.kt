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

/**
 * 单个可解析模块持有的 low-level CFIR 解析组件集合。
 */
internal class LLCfirModuleResolveComponents(
    /**
     * 当前组件集合所属 Analysis API 模块。
     */
    val module: CaModule,
    /**
     * project 级共享的全局解析组件。
     */
    val globalResolveComponents: LLCfirGlobalResolveComponents,
    /**
     * 当前模块使用的 CFIR scope provider。
     */
    val scopeProvider: CfirScopeProvider
) {
    /**
     * 当前模块的 CFIR 文件缓存。
     */
    val cache: ModuleFileCache = ModuleFileCacheImpl(this)

    /**
     * 将 PSI 文件构建为 raw CFIR 文件的 builder。
     */
    val cfirFileBuilder: LLCfirFileBuilder = LLCfirFileBuilder(this)

    /**
     * 当前模块的 lazy declaration resolver。
     */
    val cfirModuleLazyDeclarationResolver = LLCfirModuleLazyDeclarationResolver(this)

    /**
     * 当前模块的 scope session provider，受 PSI 与 project root 修改计数失效。
     */
    val scopeSessionProvider: LLCfirScopeSessionProvider = LLCfirScopeSessionProvider.create(
        globalResolveComponents.project,
        invalidationTrackers = listOf(
            PsiModificationTracker.MODIFICATION_COUNT,
            ProjectRootModificationTracker.getInstance(globalResolveComponents.project),
        )
    )

    /**
     * 文件结构缓存，用于按 PSI 定位可 lazy resolve 的 CFIR 结构元素。
     */
    val fileStructureCache: FileStructureCache = FileStructureCache(this)

    /**
     * 从 PSI 元素构建或恢复 CFIR 元素的 builder。
     */
    val elementsBuilder = CfirElementBuilder(this)

    /**
     * 基于文件结构缓存执行 diagnostics 收集的组件。
     */
    val diagnosticsCollector = DiagnosticsCollector(fileStructureCache)

    /**
     * 反向指向拥有这些组件的可解析模块 session。
     */
    lateinit var session: LLCfirResolvableModuleSession
}
