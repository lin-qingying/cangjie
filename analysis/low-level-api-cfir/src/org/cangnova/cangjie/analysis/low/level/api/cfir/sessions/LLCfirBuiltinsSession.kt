/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes

/**
 * Builtins 模块使用的 low-level CFIR session。
 *
 * Builtins session 按库类 session 处理，不参与源码 lazy resolve，只承载内建类型和 builtins symbol provider。
 *
 * session 的组件是分步注册的：`CfirProvider` / `CfirSymbolProvider` 最后才挂上。建 session 期间若同线程
 * 回到“取 session”的入口（见 `org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.BuiltinsSessionCreationGuard`），
 * 拿到的是**半成品 session**，只能使用重入点之前已注册的组件。为了让这种误用在远处也能自解释，
 * [toString] 会带出构造状态，`ArrayMapAccessor` 缺组件时的错误信息里因此能直接看出这个 session 还没建完。
 */
class LLCfirBuiltinsSession @PrivateSessionConstructor constructor(
    caModule: CaModule,
    builtinTypes: CfirBuiltinTypes,
) : LLCfirLibraryLikeSession(caModule, builtinTypes) {
    /**
     * 构造状态；[COMPLETED] 之前 session 只能算半成品。
     */
    @Volatile
    private var constructionState: ConstructionState = ConstructionState.IN_PROGRESS

    /**
     * 当前 session 是否仍处于构造中（组件可能还没注册齐）。
     */
    val isUnderConstruction: Boolean
        get() = constructionState == ConstructionState.IN_PROGRESS

    /**
     * 标记构造完成；只应由创建方在全部组件注册完毕后调用一次。
     */
    fun markConstructionCompleted() {
        constructionState = ConstructionState.COMPLETED
    }

    /**
     * 返回带构造状态的 session 描述，诊断里可直接看出是否为半成品。
     */
    override fun toString(): String = "${super.toString()}, construction=$constructionState"

    /**
     * session 构造状态。
     */
    enum class ConstructionState {
        /** 组件仍在注册中，只保证重入点之前的组件可读。 */
        IN_PROGRESS,

        /** 全部组件已注册，session 可完整使用。 */
        COMPLETED,
    }
}