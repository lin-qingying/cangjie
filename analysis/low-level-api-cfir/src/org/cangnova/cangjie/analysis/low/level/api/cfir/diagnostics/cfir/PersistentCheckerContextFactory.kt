/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.fir

import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.LLCfirReturnTypeCalculatorWithJump
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.LLImplicitBodyResolveComputationSession
import org.cangnova.cangjie.cfir.analysis.checkers.context.PersistentCheckerContext
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder

internal object PersistentCheckerContextFactory {
    fun createEmptyPersistenceCheckerContext(sessionHolder: SessionAndScopeSessionHolder): PersistentCheckerContext {
        val returnTypeCalculator = LLCfirReturnTypeCalculatorWithJump(
            scopeSession = sessionHolder.scopeSession,
            implicitBodyResolveComputationSession = LLImplicitBodyResolveComputationSession(),
        )

        return PersistentCheckerContext(sessionHolder, returnTypeCalculator)
    }
}