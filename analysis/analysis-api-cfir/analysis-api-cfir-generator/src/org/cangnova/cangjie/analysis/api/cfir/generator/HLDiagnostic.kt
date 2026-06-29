/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator

import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticData
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model.DiagnosticParameter
import kotlin.reflect.KType

/**
 * 生成公开诊断 API 时使用的高层诊断模型。
 *
 * 该模型把 CFIR 原始诊断定义、公开接口名、实现类名和参数转换结果集中到一个结构中，
 * 供接口、实现和转换器 renderer 复用。
 */
data class HLDiagnostic(
    /**
     * CFIR checker 生成器中的原始诊断定义。
     */
    val original: DiagnosticData,
    /**
     * 废弃诊断拆分出的严重级别；普通诊断为 null。
     */
    val severity: HLDiagnosticSeverity?,
    /**
     * Analysis API 公开诊断接口名。
     */
    val className: String,
    /**
     * Analysis API CFIR 诊断实现类名。
     */
    val implClassName: String,
    /**
     * 该诊断公开给 Analysis API 的参数列表。
     */
    val parameters: List<HLDiagnosticParameter>,
)

/**
 * 废弃类诊断在高层 API 中拆分出的严重级别。
 */
enum class HLDiagnosticSeverity {
    /**
     * 错误级废弃诊断。
     */
    ERROR,

    /**
     * 警告级废弃诊断。
     */
    WARNING,
}

/**
 * 一组可生成公开诊断 API 的高层诊断模型。
 */
data class HLDiagnosticList(
    /**
     * 经过转换和拆分后的诊断模型列表。
     */
    val diagnostics: List<HLDiagnostic>,
)

/**
 * 公开诊断参数的高层模型。
 */
data class HLDiagnosticParameter(
    /**
     * CFIR 原始诊断参数定义。
     */
    val original: DiagnosticParameter,
    /**
     * Analysis API 公开参数名。
     */
    val name: String,
    /**
     * Analysis API 公开参数类型。
     */
    val type: KType,
    /**
     * CFIR 生成诊断类中原始参数的字段名。
     */
    val originalParameterName: String,
    /**
     * 从 CFIR 参数值转换到公开参数值的转换规则。
     */
    val conversion: HLParameterConversion,
    /**
     * 生成转换代码时需要额外添加的导入。
     */
    val importsToAdd: List<String>
)
