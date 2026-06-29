/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.util.body
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
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
        if (element.resolvePhase > newPhase) return
        if (!isTargetDeclaration) {
            // phase update for target declaration happens as a declaration publication event after resolve is finished
            if (element.resolvePhase < newPhase) {
                @OptIn(ResolveStateAccess::class)
                element.resolveState = newPhase.asResolveState()
            }
        }

        if (element is CfirTypeParameterRefsOwner) {
            element.typeParameters.forEach { typeParameter ->
                // if it is not a type parameter of outer declaration
                if (typeParameter is CfirTypeParameter) {
                    updatePhaseForNonLocals(typeParameter, newPhase, isTargetDeclaration = false)
                }
            }
        }

        when (element) {
            is CfirClass -> {
            }
            is CfirFunction -> {
                element.valueParameters.forEach { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            is CfirProperty -> {
                element.getter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
                element.setter?.let { updatePhaseForNonLocals(it, newPhase, isTargetDeclaration = false) }
            }
            else -> {}
        }
    }
}

/**
 * 遍历 body 内局部元素，并把所有带解析状态的局部节点标记为 body resolve 完成。
 */
private object LocalElementPhaseUpdatingTransformer : CfirVisitorVoid() {
    /**
     * 更新当前 [element] 的解析状态后继续访问其子节点。
     */
    override fun visitElement(element: CfirElement) {
        if (element is CfirElementWithResolveState) {
            @OptIn(ResolveStateAccess::class)
            element.resolveState = CfirResolvePhase.BODY_RESOLVE.asResolveState()
        }

        element.acceptChildren(this)
    }
}
