@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDeserializationStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDiagnosticsStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLSessionStatistics

/**
 * 取当前工程的 low-level 统计服务；统计未启用时为 `null`。
 *
 * 这是 low-level 模块内访问统计域的统一入口：统计开关由 registry key 控制、provider 由
 * 平台提供，两者任一缺失都意味着"没有统计后端"，此时调用方必须零开销地继续正常路径。
 * 各调用方应把结果缓存成字段，不要在热路径上反复调用。
 */
internal fun Project.llStatistics(): LLStatisticsService? = LLStatisticsService.getInstance(this)

/**
 * 取当前工程的诊断收集统计域；统计未启用时为 `null`。
 */
internal fun Project.llDiagnosticsStatistics(): LLDiagnosticsStatistics? = llStatistics()?.diagnostics

/**
 * 取当前工程的 session 创建统计域；统计未启用时为 `null`。
 */
internal fun Project.llSessionStatistics(): LLSessionStatistics? = llStatistics()?.sessions

/**
 * 取当前工程的 stub 反序列化统计域；统计未启用时为 `null`。
 */
internal fun Project.llDeserializationStatistics(): LLDeserializationStatistics? = llStatistics()?.deserialization
