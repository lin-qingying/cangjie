/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirSession
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.resolve.ScopeSession
import org.cangnova.cangjie.cfir.resolve.transformers.CfirJumpingPhaseComputationSessionForLocalClassesProvider
import org.cangnova.cangjie.cfir.resolve.transformers.StatusComputationSession
import org.cangnova.cangjie.cfir.resolve.transformers.SupertypeComputationSession
import org.cangnova.cangjie.cfir.resolve.transformers.plugin.CompilerRequiredAnnotationsComputationSession

@OptIn(CfirImplementationDetail::class)
internal object LLJumpingPhaseComputationSessionForLocalClassesProvider : CfirJumpingPhaseComputationSessionForLocalClassesProvider() {
    override fun compilerRequiredAnnotationPhaseSession(): CompilerRequiredAnnotationsComputationSession {
        return LLCompilerRequiredAnnotationsComputationSessionLocalClassesAware()
    }

    override fun superTypesPhaseSession(): SupertypeComputationSession {
        return LLSupertypeComputationSessionLocalClassesAware()
    }

    override fun statusPhaseSession(
        useSiteSession: CfirSession,
        useSiteScopeSession: ScopeSession,
        designationMapForLocalClasses: Map<CfirClassLikeDeclaration, CfirClassLikeDeclaration?>,
    ): StatusComputationSession = LLStatusComputationSessionLocalClassesAware(
        useSiteSession,
        useSiteScopeSession,
    )
}
