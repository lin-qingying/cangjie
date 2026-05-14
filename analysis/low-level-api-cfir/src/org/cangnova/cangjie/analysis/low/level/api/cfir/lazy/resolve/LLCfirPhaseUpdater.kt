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

internal object LLCfirPhaseUpdater {
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

    private fun updateFunctionBody(target: CfirFunction) {
        target.body?.accept(LocalElementPhaseUpdatingTransformer)
    }

    private fun updateFunctionSignatureBody(target: CfirFunction) {
        target.valueParameters.forEach { it.defaultValue?.accept(LocalElementPhaseUpdatingTransformer) }
    }

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

private object LocalElementPhaseUpdatingTransformer : CfirVisitorVoid() {
    override fun visitElement(element: CfirElement) {
        if (element is CfirElementWithResolveState) {
            @OptIn(ResolveStateAccess::class)
            element.resolveState = CfirResolvePhase.BODY_RESOLVE.asResolveState()
        }

        element.acceptChildren(this)
    }
}
