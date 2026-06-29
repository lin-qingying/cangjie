/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration

/**
 * diagnostics 收集前的扩展钩子，用于测试或宿主在访问元素前观测 checker context。
 */
abstract class BeforeElementDiagnosticCollectionHandler: CfirSessionComponent {
    /**
     * 在对某个 CFIR 元素执行诊断收集前调用。
     */
    open fun beforeCollectingForElement(element: CfirElement) {}

    /**
     * 在 collector 进入嵌套声明前调用，并提供当前 checker context。
     */
    open fun beforeGoingNestedDeclaration(declaration: CfirDeclaration, context: CheckerContext) {}
}

/**
 * 从 CFIR session 中取得可选的 before-element diagnostics handler。
 */
val CfirSession.beforeElementDiagnosticCollectionHandler: BeforeElementDiagnosticCollectionHandler? by CfirSession.nullableSessionComponentAccessor()
