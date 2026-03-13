/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.resolve

import org.cangjie.cfir.CfirSession
import org.cangjie.cfir.CfirSessionComponent
import org.cangjie.cfir.declarations.CfirClass
import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.utils.isSealed
import org.cangjie.cfir.diagnostics.ConeDiagnostic
import org.cangjie.cfir.expressions.CfirResolvedQualifier
import org.cangjie.cfir.scopes.CfirScope
import org.cangjie.cfir.session.CfirSessionComponent
import org.cangjie.cfir.symbols.impl.CfirRegularClassSymbol
import org.cangjie.cfir.types.ConeKotlinType
import org.cangjie.cfir.types.CfirTypeRef

abstract class CfirTypeResolver : CfirSessionComponent {
    abstract fun resolveType(
        typeRef: CfirTypeRef,
        configuration: TypeResolutionConfiguration,
        // TODO: Consider putting other parameters to TypeResolutionConfiguration
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        resolveDeprecations: Boolean,
        supertypeSupplier: SupertypeSupplier,
        expandTypeAliases: Boolean = true,
    ): CfirTypeResolutionResult

    abstract fun resolveTypeOnDoubleColonLHS(
        qualifier: CfirResolvedQualifier,
        configuration: TypeResolutionConfiguration,
    ): DoubleColonLHS.Type?
}

class TypeResolutionConfiguration private constructor(
    val scopes: Iterable<CfirScope>,
    val containingClassDeclarations: List<CfirClass>,
    // Note: sometimes we don't have useSiteFile in IDE context
    val useSiteFile: CfirFile?,
    val topContainer: CfirDeclaration? = null,
    // For `x: MySealed` and `x is MySubClass` instead of `x is MySealed.MySubClass`
    // the class symbol would point to MySealed.
    // It's guaranteed to be sealed.
    val sealedClassForContextSensitiveResolution: CfirRegularClassSymbol? = null,
) {
    constructor(
        scopes: Iterable<CfirScope>,
        containingClassDeclarations: List<CfirClass>,
        useSiteFile: CfirFile?,
        topContainer: CfirDeclaration? = null,
    ) : this(scopes, containingClassDeclarations, useSiteFile, topContainer, sealedClassForContextSensitiveResolution = null)

    companion object {
        fun createForContextSensitiveResolution(
            containingClassDeclarations: List<CfirClass>,
            useSiteFile: CfirFile,
            topContainer: CfirDeclaration?,
            sealedClassForContextSensitiveResolution: CfirRegularClassSymbol,
        ): TypeResolutionConfiguration {
            require(sealedClassForContextSensitiveResolution.isSealed)
            return TypeResolutionConfiguration(
                scopes = emptyList(),
                containingClassDeclarations, useSiteFile, topContainer,
                sealedClassForContextSensitiveResolution,
            )
        }
    }
}

data class CfirTypeResolutionResult(
    val type: ConeKotlinType,
    val diagnostic: ConeDiagnostic?,
    val resolvedSymbolOrigin: CfirResolvedSymbolOrigin? = null,
)

val CfirSession.typeResolver: CfirTypeResolver by CfirSession.sessionComponentAccessor()
