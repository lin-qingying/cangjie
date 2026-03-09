package org.cangjie.cfir.declarations

/**
 * 解析阶段枚举。
 *
 * CFIR 采用分阶段解析，每个声明跟踪自己当前已完成的解析阶段。
 * 参考 Kotlin FIR 的阶段系统，适配仓颉语言特性。
 */
enum class CfirResolvePhase {
    /** PSI/LightTree → CFIR 转换完成，尚未进行任何解析 */
    RAW_CFIR,

    /** 解析 import 语句 */
    IMPORTS,

    /** 解析父类型（class <: SuperClass） */
    SUPER_TYPES,

    /** 解析显式类型（参数类型、返回类型、字段类型） */
    TYPES,

    /** 解析声明状态（public/private/open/abstract/mut 等修饰符） */
    STATUS,

    /** 解析 extend 声明（仓颉特有：extend Type <: Interface） */
    EXTENSIONS,

    /** 推断隐式类型 */
    IMPLICIT_TYPES,

    /** 解析函数体和表达式 */
    BODY_RESOLVE,

    /** 运行诊断检查器 */
    CHECKERS;

    val next: CfirResolvePhase
        get() {
            val values = entries
            val nextOrdinal = ordinal + 1
            return if (nextOrdinal < values.size) values[nextOrdinal] else this
        }
}
