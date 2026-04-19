/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.getAllClassLikeSymbolsByClassIdOrSingle
import org.cangnova.cangjie.cfir.CfirNameConflictsTracker
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

internal class LLNameConflictsTracker(private val session: LLCfirSession) : CfirNameConflictsTracker() {
    private data class LLClassifierRedeclaration(override val classifierSymbol: CfirClassLikeSymbol<*>) : ClassifierRedeclaration() {
        // Grabbing the containing file via the symbol provider is non-trivial. Specifying it is optional, and it can later be retrieved
        // separately.
        override val containingFile: CfirFile? get() = null
    }

    override fun getClassifierRedeclarations(classId: ClassId): Collection<ClassifierRedeclaration> {
        // As noted in the KDoc of `getClassifierRedeclarations`, Java redeclarations should not be returned by this component. As such,
        // we limit the scope to Kotlin symbols by taking the CFIR provider's symbol provider.
        val symbolProvider = session.cfirProvider.symbolProvider

        // While redeclarations are rare, we don't know whether a given class is redeclared somewhere else in the whole module. So this
        // function will be called once for each top-level classifier. Still, we don't cache the result since checkers are only run on
        // specific files, not all files visited by lazy resolution.
        return symbolProvider
            .getAllClassLikeSymbolsByClassIdOrSingle(classId)
            .takeIf { it.size >= 2 }
            .orEmpty()
            .map { LLClassifierRedeclaration(it) }
    }

    override fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: CfirClassLikeSymbol<*>,
        newSymbolFile: CfirFile,
        prevSymbol: CfirClassLikeSymbol<*>,
        prevSymbolFile: CfirFile?,
    ) {
        // In the Analysis API, classifier redeclarations are resolved from symbol providers, so we don't need to register them here.
        // Registration-based conflict trackers require the whole module to be analyzed, which is not performant in the Analysis API.
    }
}
