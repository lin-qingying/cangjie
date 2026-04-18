/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirReplSnippetSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

internal class LLCfirBuiltinsSessionProvider(override val symbolProvider: CfirSymbolProvider) : CfirProvider() {
    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? =
        symbolProvider.getClassLikeSymbolByClassId(classId)?.fir

    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile = shouldNotBeCalled()

    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? = null
    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? = null
    override fun getCfirReplSnippetContainerFile(symbol: CfirReplSnippetSymbol): CfirFile? = null
    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = emptyList()

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> = shouldNotBeCalled()

    private fun shouldNotBeCalled(): Nothing = error("Should not be called for LLCfirBuiltinsSession")
}
