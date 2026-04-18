/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.asResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.LLCfirImplicitBodyTargetResolver
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.LLCfirImplicitTypesLazyResolver
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.LLImplicitBodyResolveComputationSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.shouldBeResolvedOnImplicitTypePhase
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkCanceled
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.ScopeSession
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.ReturnTypeCalculatorWithJump
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef

internal class LLCfirReturnTypeCalculatorWithJump(
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: LLImplicitBodyResolveComputationSession,
) : ReturnTypeCalculatorWithJump(scopeSession, implicitBodyResolveComputationSession) {
    override fun resolveDeclaration(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        // Should be in sync with LLCfirImplicitBodyTargetResolver
        val hasSomethingToResolveOnImplicitTypePhase = when {
            declaration is CfirProperty -> declaration.shouldBeResolvedOnImplicitTypePhase
            else -> declaration.returnTypeRef is CfirImplicitTypeRef
        }

        if (!hasSomethingToResolveOnImplicitTypePhase) {
            return declaration.symbol.resolvedReturnTypeRef
        }

        declaration.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE.previous)

        val designation = declaration.collectDesignation().asResolveTarget()
        val computationSession = implicitBodyResolveComputationSession as LLImplicitBodyResolveComputationSession
        val resolver = LLCfirImplicitBodyTargetResolver(
            designation,
            llImplicitBodyResolveComputationSessionParameter = computationSession,
        )

        resolver.resolveDesignation()

        // Report recursion error if we found cycle during resolution
        if (computationSession.popCycledSymbolIfExists() == declaration.symbol) {
            return recursionInImplicitTypeRef(declaration)
        }

        LLCfirImplicitTypesLazyResolver.checkIsResolved(declaration)
        return declaration.returnTypeRef as CfirResolvedTypeRef
    }

    override fun tryCalculateReturnTypeOrNull(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        checkCanceled()
        return super.tryCalculateReturnTypeOrNull(declaration)
    }
}
