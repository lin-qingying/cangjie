/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.util.body
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.session.requiresDeferredCjmpFinalMatching
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid

/**
 * 统一更新 lazy resolve 完成后 CFIR 元素解析阶段的工具。
 *
 * 非局部声明的阶段由发布流程控制；body 内局部元素和默认参数值等局部结构需要在这里显式标记为
 * [CfirResolvePhase.BODY_RESOLVE]，保证后续查询不会把已处理的局部节点再次视为未解析。
 */
internal object LLCfirPhaseUpdater {
    /**
     * 将 [target] 的声明内容更新到 [newPhase] 对应的阶段状态。
     *
     * 当目标推进到 body resolve 时，还会更新默认参数值、函数体、属性访问器 body 和代码片段块中的局部元素阶段。
     */
    fun updateDeclarationContent(target: CfirElementWithResolveState, newPhase: CfirResolvePhase) {
        updatePhaseForNonLocals(target, newPhase, isTargetDeclaration = true)

        if (newPhase == CfirResolvePhase.BODY_RESOLVE) {
            updateDeclarationSignatureBody(target)

            when (target) {
                is CfirVariable -> {
                    target.initializer?.accept(LocalElementPhaseUpdatingTransformer)
                }

                is CfirProperty -> {
                    target.getter?.let(::updateFunctionBody)
                    target.setter?.let(::updateFunctionBody)
                }
                is CfirFunction -> updateFunctionBody(target)
                is CfirCodeFragment -> target.block.accept(LocalElementPhaseUpdatingTransformer)
            }
        }
    }

    /**
     * Updates the state of the [target] declaration with a partially analyzed body.
     */
    fun updatePartiallyAnalyzedDeclarationContent(target: CfirDeclaration, updateSignatureBody: Boolean, statementRange: IntRange) {
        if (updateSignatureBody) {
            updateDeclarationSignatureBody(target)
        }

        if (!statementRange.isEmpty()) {
            val statements = target.body?.statements.orEmpty()
            require(statements.size > statementRange.last)

            val statementsToUpdate = statements.subList(statementRange.first, statementRange.last + 1)
            statementsToUpdate.forEach { it.accept(LocalElementPhaseUpdatingTransformer) }
        }
    }

    /**
     * 更新声明签名中需要 body resolve 才完成的部分。
     *
     * 当前主要覆盖函数、构造函数和属性访问器参数的默认值。
     */
    private fun updateDeclarationSignatureBody(target: CfirElementWithResolveState) {
        when (target) {
            is CfirConstructor -> updateFunctionSignatureBody(target)
            is CfirFunction -> {
                updateFunctionSignatureBody(target)
            }
            is CfirEnumConstructor -> {
                target.valueParameters.forEach { it.defaultValue?.accept(LocalElementPhaseUpdatingTransformer) }
            }

            is CfirProperty -> {
                target.getter?.let(::updateFunctionSignatureBody)
                target.setter?.let(::updateFunctionSignatureBody)
            }
        }
    }

    /**
     * 将 [target] 的函数体局部元素标记为 body resolve 完成。
     */
    private fun updateFunctionBody(target: CfirFunction) {
        target.body?.accept(LocalElementPhaseUpdatingTransformer)
    }

    /**
     * 将 [target] 的默认参数值局部元素标记为 body resolve 完成。
     */
    private fun updateFunctionSignatureBody(target: CfirFunction) {
        target.valueParameters.forEach { it.defaultValue?.accept(LocalElementPhaseUpdatingTransformer) }
    }

    /**
     * 更新非局部声明及其签名子结构的解析阶段。
     *
     * [isTargetDeclaration] 为 `true` 时，目标声明自身的阶段更新由 lazy resolve 发布事件负责；
     * 这里只处理类型参数、值参数、访问器等不会单独发布阶段事件的关联声明。
     */
    private fun updatePhaseForNonLocals(element: CfirElementWithResolveState, newPhase: CfirResolvePhase, isTargetDeclaration: Boolean) {
        // 注意：这里不能直接对齐 Kotlin 的 `>=`。我们的目标声明阶段可能已由发布事件提前置为
        // newPhase，若相等就返回会跳过访问器/参数等子结构的阶段更新（property accessor 会停在 TYPES）。
        if (element.resolvePhase > newPhase) return
        if (!isTargetDeclaration) {
            // phase update for target declaration happens as a declaration publication event after resolve is finished
            if (element.resolvePhase < newPhase) {
                @OptIn(ResolveStateAccess::class)
                element.resolveState = newPhase.asResolveState()
            }
        }

        val publishPatternBindings = element is CfirPatternVariable && newPhase.publishesCjmpPatternBindings(element.moduleData.session)
        forEachSignatureInternal(element, publishPatternBindings) {
            updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false)
        }
    }

    /** updater 与声明事务使用同一直接内部节点清单，不保存外围泛型参数或独立成员声明的状态。 */
    fun forEachSignatureInternal(
        element: CfirElementWithResolveState,
        publishPatternBindings: Boolean,
        action: (CfirElementWithResolveState) -> Unit,
    ) {
        if (element is CfirTypeParameterRefsOwner) {
            element.typeParameters.filterIsInstance<CfirTypeParameter>().forEach(action)
        }
        when (element) {
            // 普通 BODY_RESOLVE 由每个非局部叶子自己的目标锁发布；CJMP 配对以模式根为事务。
            is CfirPatternVariable -> if (publishPatternBindings) element.pattern.bindingVariables().forEach(action)
            is CfirFunction -> element.valueParameters.forEach(action)
            is CfirEnumConstructor -> element.valueParameters.forEach(action)
            is CfirProperty -> {
                element.getter?.let(action)
                element.setter?.let(action)
            }
            else -> Unit
        }
    }
}

/** 只在 active specific 配对阶段从模式根发布签名叶子阶段。 */
internal fun CfirResolvePhase.publishesCjmpPatternBindings(session: CfirSession): Boolean = when (this) {
    CfirResolvePhase.CJMP_MATCHING -> session.isCjmpSpecificCompilationEnabled()
    CfirResolvePhase.CJMP_FINAL_MATCHING -> session.requiresDeferredCjmpFinalMatching()
    else -> false
}

/**
 * 遍历 body 内局部元素，并把所有带解析状态的局部节点标记为 body resolve 完成。
 */
private object LocalElementPhaseUpdatingTransformer : CfirVisitorVoid() {
    /**
     * 更新当前 [element] 的解析状态后继续访问其子节点。
     */
    override fun visitElement(element: CfirElement) {
        if (element is CfirPatternBindingVariable) {
            // local phase 发布在完整 completion 后执行，Deferred 不能被误标为 BODY_RESOLVE。
            org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkReturnTypeRefIsResolved(element)
        }
        if (element is CfirElementWithResolveState) {
            @OptIn(ResolveStateAccess::class)
            element.resolveState = CfirResolvePhase.BODY_RESOLVE.asResolveState()
        }

        element.acceptChildren(this)
    }
}
