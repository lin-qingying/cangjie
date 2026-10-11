package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule

/**
 * [CaModule] 的种类。
 *
 * 这是"模块属于哪一类"的单一真源：JFR 分析事件（[LLFlightRecorder]）与 OpenTelemetry 指标
 * （session 创建耗时）都按它分类。此前两处各自维护一份 `when` 分类，模块种类新增时必须改两处，
 * 且两边一旦漂移，同一次操作会在两个遥测通道里落到不同类别上。
 *
 * 继承顺序即判定顺序，必须与 [CaDanglingFileModule] 是 [CaSourceModule] 子类型这一事实一致：
 * dangling 文件是源码模块的一种，但它的 session 创建成本与普通源码模块差一个量级，必须先判。
 *
 * @property metricSuffix 指标名中的类别段。
 * @property jfrCode JFR 事件中的紧凑类别编号。
 */
enum class CaModuleKind(
    val metricSuffix: String,
    val jfrCode: Byte,
) {
    /** 普通源码模块。 */
    SOURCE("source", 0),

    /** 游离文件模块：挂在某个上下文模块下、单文件分析。 */
    DANGLING_FILE("danglingFile", 1),

    /** 不在 content root 下的模块。 */
    NOT_UNDER_CONTENT_ROOT("notUnderContentRoot", 2),

    /** 兜底依赖模块：合并全部库，单次分析后即失效。 */
    FALLBACK_DEPENDENCIES("fallbackDependencies", 3),

    /** 已编译二进制库模块（`preferBinary` 时按二进制创建 session）。 */
    LIBRARY("library", 4),

    /** 库源码模块。 */
    LIBRARY_SOURCE("librarySource", 5),

    /** 内建模块（stdlib 等 `.cjo`）。 */
    BUILTINS("builtins", 6),

    /** 未识别的模块种类；只出现在 JFR 通道，指标通道不接受该值。 */
    UNKNOWN("unknown", -1),
}

/**
 * 返回当前模块的种类。
 *
 * 未匹配任何已知种类时返回 [CaModuleKind.UNKNOWN]：JFR 事件沿用 -1（既有编号，不可变更），
 * 指标侧则不应把未识别种类算进任何 session 创建耗时，由调用方决定是否跳过。
 */
fun CaModule.moduleKind(): CaModuleKind = when (this) {
    is CaDanglingFileModule -> CaModuleKind.DANGLING_FILE
    is CaSourceModule -> CaModuleKind.SOURCE
    is CaNotUnderContentRootModule -> CaModuleKind.NOT_UNDER_CONTENT_ROOT
    is CaLibraryFallbackDependenciesModule -> CaModuleKind.FALLBACK_DEPENDENCIES
    is CaLibrarySourceModule -> CaModuleKind.LIBRARY_SOURCE
    is CaLibraryModule -> CaModuleKind.LIBRARY
    is CaBuiltinsModule -> CaModuleKind.BUILTINS
    else -> CaModuleKind.UNKNOWN
}
