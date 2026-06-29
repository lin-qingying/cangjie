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

/**
 * 将 CFIR 原始诊断定义转换为 Analysis API 诊断生成模型。
 *
 * 转换过程中会处理废弃诊断的错误/警告拆分、公开诊断类名生成以及参数类型转换。
 */
object HLDiagnosticConverter {
    /**
     * 转换完整的 CFIR 诊断列表。
     */
    fun convert(diagnosticList: DiagnosticList): HLDiagnosticList {
        return HLDiagnosticList(diagnosticList.allDiagnostics.flatMap(::convertDiagnostic))
    }

    /**
     * 转换单个诊断定义。
     *
     * 普通诊断生成一个高层诊断；废弃诊断按严重级别生成错误和警告两个高层诊断。
     */
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

    /**
     * 转换单个诊断参数，并记录原始生成诊断类中的参数字段名。
     */
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

    /**
     * 生成普通诊断的公开接口名。
     */
    private fun RegularDiagnosticData.getHLDiagnosticClassName(): String = name.sanitizeName()

    /**
     * 生成普通诊断的实现类名。
     */
    private fun RegularDiagnosticData.getHLDiagnosticImplClassName(): String =
        "${getHLDiagnosticClassName()}Impl"

    /**
     * 生成废弃诊断在指定严重级别下的公开接口名。
     */
    private fun DeprecationDiagnosticData.getHLDiagnosticClassName(severity: HLDiagnosticSeverity): String {
        val diagnosticName = "${name}_${severity.name}"
        return diagnosticName.sanitizeName()
    }

    /**
     * 生成废弃诊断在指定严重级别下的实现类名。
     */
    private fun DeprecationDiagnosticData.getHLDiagnosticImplClassName(severity: HLDiagnosticSeverity): String {
        return "${getHLDiagnosticClassName(severity)}Impl"
    }

    /**
     * 将诊断定义中的下划线大写名称转换为公开 API 使用的 PascalCase 名称。
     */
    private fun String.sanitizeName(): String =
        lowercase()
            .split('_')
            .joinToString(separator = "") {
                it.replaceFirstChar(Char::uppercaseChar)
            }

}

/**
 * CFIR 诊断参数到 Analysis API 公开参数的转换规则创建器。
 */
internal object CfirToCjConversionCreator {
    /**
     * 根据原始参数类型创建对应的公开参数转换规则。
     */
    fun createConversion(type: KType): HLParameterConversion {
        val nullable = type.isMarkedNullable
        val kClass = type.classifier as KClass<*>
        return tryMapAllowedType(kClass)
            ?: tryMapPsiElementType(kClass)
            ?: tryMapCfirTypeToCjType(kClass, nullable)
            ?: tryMapPlatformType(type, kClass)
            ?: error("Unsupported type $type, consider add corresponding mapping")
    }

    /**
     * 返回运行时参数分派转换器需要覆盖的全部 CFIR 类型映射。
     */
    fun getAllConverters(conversionForCollectionValues: HLParameterConversion): Map<KClass<*>, HLParameterConversion> {
        return buildMap {
            putAll(typeMapping)
            put(
                Collection::class,
                HLCollectionParameterConversion("value", conversionForCollectionValues)
            )
        }
    }

    /**
     * 尝试把 CFIR 内部类型映射为 Analysis API 公共类型。
     */
    private fun tryMapCfirTypeToCjType(kClass: KClass<*>, nullable: Boolean): HLParameterConversion? {
        return if (nullable) {
            nullableTypeMapping[kClass] ?: typeMapping[kClass]
        } else {
            typeMapping[kClass]
        }
    }

    /**
     * 尝试保留允许直接暴露的简单参数类型。
     */
    private fun tryMapAllowedType(kClass: KClass<*>): HLParameterConversion? {
        if (kClass in allowedTypesWithoutTypeParams) return HLIdParameterConversion
        return null
    }

    /**
     * 根据类型简单名派生集合元素转换 lambda 的参数名。
     */
    private fun KType.toParameterName(): String {
        return kClass.simpleName!!.replaceFirstChar(Char::lowercaseChar)
    }

    /**
     * 尝试处理平台集合类型等非 CFIR 专有参数。
     */
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

    /**
     * 尝试保留 PSI 元素类型参数。
     */
    private fun tryMapPsiElementType(kClass: KClass<*>): HLParameterConversion? {
        if (kClass.isSubclassOf(PsiElement::class)) {
            return HLIdParameterConversion
        }
        return null
    }

    /**
     * 可空 CFIR 类型到公开类型的特殊映射。
     */
    private val nullableTypeMapping: Map<KClass<*>, HLFunctionCallConversion> = emptyMap()

    /**
     * CFIR 内部类型到 Analysis API 公共类型的直接映射。
     */
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

    /**
     * 无泛型参数且允许原样暴露到公开诊断 API 的类型集合。
     */
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

    /**
     * 反射类型对应的 Kotlin class。
     */
    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>
}
