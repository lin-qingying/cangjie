/*
 * Copyright 2010-2024 JetBrains s.r.o. and CangJie Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DeprecationDiagnosticData
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticData
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticList
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticParameter
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.RegularDiagnosticData
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf

object HLDiagnosticConverter {
    fun convert(diagnosticList: DiagnosticList): HLDiagnosticList {
        return HLDiagnosticList(diagnosticList.allDiagnostics.flatMap(::convertDiagnostic))
    }

    private fun convertDiagnostic(diagnostic: DiagnosticData): List<HLDiagnostic> {
        return when (diagnostic){
            is RegularDiagnosticData -> listOf(
                HLDiagnostic(
                    original = diagnostic,
                    severity = null,
                    className = diagnostic.getHLDiagnosticClassName(),
                    implClassName = diagnostic.getHLDiagnosticImplClassName(),
                    parameters = diagnostic.parameters.mapIndexed(::convertParameter)
                )
            )
            is DeprecationDiagnosticData -> listOf(HLDiagnosticSeverity.ERROR, HLDiagnosticSeverity.WARNING).map {
                HLDiagnostic(
                    original = diagnostic,
                    severity = it,
                    className = diagnostic.getHLDiagnosticClassName(it),
                    implClassName = diagnostic.getHLDiagnosticImplClassName(it),
                    parameters = diagnostic.parameters.mapIndexed(::convertParameter)
                )
            }
        }
    }

    private fun convertParameter(index: Int, diagnosticParameter: DiagnosticParameter): HLDiagnosticParameter {
        val conversion = CfirToCjConversionCreator.createConversion(diagnosticParameter.type)
        val convertedType = conversion.convertType(diagnosticParameter.type)
        return HLDiagnosticParameter(
            name = diagnosticParameter.name,
            conversion = conversion,
            originalParameterName = ('a' + index).toString(),
            type = convertedType,
            original = diagnosticParameter,
            importsToAdd = conversion.importsToAdd
        )
    }

    private fun RegularDiagnosticData.getHLDiagnosticClassName(): String = name.sanitizeName()

    private fun RegularDiagnosticData.getHLDiagnosticImplClassName(): String =
        "${getHLDiagnosticClassName()}Impl"

    private fun DeprecationDiagnosticData.getHLDiagnosticClassName(severity: HLDiagnosticSeverity): String {
        val diagnosticName = "${name}_${severity.name}"
        return diagnosticName.sanitizeName()
    }

    private fun DeprecationDiagnosticData.getHLDiagnosticImplClassName(severity: HLDiagnosticSeverity): String {
        return "${getHLDiagnosticClassName(severity)}Impl"
    }

    private fun String.sanitizeName(): String =
        lowercase()
            .split('_')
            .joinToString(separator = "") {
                it.replaceFirstChar(Char::uppercaseChar)
            }

}

internal object CfirToCjConversionCreator {
    fun createConversion(type: KType): HLParameterConversion {
        val nullable = type.isMarkedNullable
        val kClass = type.classifier as KClass<*>
        return tryMapAllowedType(kClass)
            ?: tryMapPsiElementType(kClass)
            ?: tryMapCfirTypeToCjType(kClass, nullable)
            ?: tryMapPlatformType(type, kClass)
            ?: error("Unsupported type $type, consider add corresponding mapping")
    }

    fun getAllConverters(conversionForCollectionValues: HLParameterConversion): Map<KClass<*>, HLParameterConversion> {
        return buildMap {
            putAll(typeMapping)
            put(
                Collection::class,
                HLCollectionParameterConversion("value", conversionForCollectionValues)
            )
        }
    }

    private fun tryMapCfirTypeToCjType(kClass: KClass<*>, nullable: Boolean): HLParameterConversion? {
        return if (nullable) {
            nullableTypeMapping[kClass] ?: typeMapping[kClass]
        } else {
            typeMapping[kClass]
        }
    }

    private fun tryMapAllowedType(kClass: KClass<*>): HLParameterConversion? {
        if (kClass in allowedTypesWithoutTypeParams) return HLIdParameterConversion
        return null
    }

    private fun KType.toParameterName(): String {
        return kClass.simpleName!!.replaceFirstChar(Char::lowercaseChar)
    }

    private fun tryMapPlatformType(type: KType, kClass: KClass<*>): HLParameterConversion? {
        if (kClass.isSubclassOf(Collection::class)) {
            val elementType = type.arguments.single().type ?: return HLIdParameterConversion
            return HLCollectionParameterConversion(
                parameterName = elementType.toParameterName(),
                mappingConversion = createConversion(elementType)
            )
        }
        return null
    }

    private fun tryMapPsiElementType(kClass: KClass<*>): HLParameterConversion? {
        if (kClass.isSubclassOf(PsiElement::class)) {
            return HLIdParameterConversion
        }
        return null
    }

    private val nullableTypeMapping: Map<KClass<*>, HLFunctionCallConversion> = emptyMap()

    private val typeMapping: Map<KClass<*>, HLFunctionCallConversion> = mapOf(
        CfirTypeParameterSymbol::class to HLFunctionCallConversion(
            "cfirSymbolBuilder.classifierBuilder.buildTypeParameterSymbol({0})",
            CaTypeParameterSymbol::class.createType()
        ),
        ConeCangJieType::class to HLFunctionCallConversion(
            "cfirSymbolBuilder.typeBuilder.buildType({0})",
            CaType::class.createType()
        )
    )

    private val allowedTypesWithoutTypeParams = setOf(
        Boolean::class,
        String::class,
        Int::class,
        Long::class,
        Name::class,
        FqName::class,
        CjKeywordToken::class,
        Visibility::class,
    )

    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>
}
