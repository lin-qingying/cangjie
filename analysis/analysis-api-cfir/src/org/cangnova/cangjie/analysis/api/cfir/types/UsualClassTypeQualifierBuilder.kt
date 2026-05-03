/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.utils.asPublicTypeProjection
import org.cangnova.cangjie.analysis.api.types.CaResolvedClassTypeQualifier
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.toSequence
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.tryCollectDesignationWithOptionalFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.withConeTypeEntry
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

internal object UsualClassTypeQualifierBuilder {
    fun buildQualifiers(
        coneType: ConeClassifierType,
        builder: CaSymbolByCfirBuilder
    ): List<CaResolvedClassTypeQualifier> {

        val classSymbolToRender = coneType.lookupTag.toSymbol(builder.rootSession)
            ?: errorWithCfirSpecificEntries("ConeClassLikeType is not resolved to symbol for on-error type", coneType = coneType) {
                withEntry("useSiteSession", builder.rootSession) { it.toString() }
            }

        val designation = classSymbolToRender.cfir.let {
            val nonLocalDesignation = it.tryCollectDesignationWithOptionalFile()
            nonLocalDesignation?.toSequence(includeTarget = true)?.toList() ?: collectDesignationPathForLocal(it)
        }.filterIsInstance<CfirClassLikeDeclaration>()

        var typeParametersLeft = coneType.typeArguments.size

        fun needToRenderTypeParameters(index: Int): Boolean {
            if (typeParametersLeft <= 0) return false
            return index == designation.lastIndex || designation[index].isInner || designation[index + 1].isInner
        }

        val result = mutableListOf<CaResolvedClassTypeQualifier>()
        designation.forEachIndexed { index, currentClass ->
            val typeParameters = if (needToRenderTypeParameters(index)) {
                val typeParametersCount = currentClass.typeParameters.count { it is CfirTypeParameter }
                val begin = typeParametersLeft - typeParametersCount
                val end = typeParametersLeft
                checkWithAttachment(begin >= 0, { "Unexpected number of type parameters" }) {
                    withEntry("designation", designation.toString())
                    withCfirEntry("currentClass", currentClass)
                    withConeTypeEntry("coneType", coneType)
                }

                typeParametersLeft -= typeParametersCount
                coneType.typeArguments.slice(begin until end).map { it.asPublicTypeProjection(builder.analysisSession) }
            } else emptyList()

            val symbol = builder.classifierBuilder.buildClassLikeSymbol(currentClass.symbol)
            result += CaCfirResolvedClassTypeQualifierImpl(
                name = symbol.name,
                typeArguments = typeParameters,
                symbol = symbol,
                token = symbol.token,
            )
        }
        return result
    }

    private fun CfirClassLikeDeclaration.collectForLocal(): List<CfirClassLikeDeclaration> {
        return listOf(this)
    }

    private fun collectDesignationPathForLocal(declaration: CfirClassLikeDeclaration): List<CfirClassLikeDeclaration> {
        return when (declaration) {
            is CfirClass,
            is CfirTypeAlias
                -> declaration.collectForLocal()
            else -> declaration.collectForLocal()
        }
    }
}

private val CaSymbolByCfirBuilder.rootSession: CfirSession
    get() = analysisSession.cfirSession

private val CfirClassLikeDeclaration.isInner: Boolean
    get() = false
