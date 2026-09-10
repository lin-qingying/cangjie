package org.cangnova.cangjie.cfir.patterns

/** 类型模式完成类型检查后的判定形式，对位官方 matchBeforeRuntime / needRuntimeTypeCheck。 */
enum class CfirTypePatternMatchingKind {
    /** 主语或目标类型尚未完成解析。 */
    UNKNOWN,
    /** 静态子类型关系已证明成功，不生成运行时条件。 */
    ALWAYS,
    /** 不存在运行期匹配可能性，生成常量 false 条件。 */
    NEVER,
    /** 需要保留 InstanceOf 等运行时类型检查。 */
    RUNTIME,
}
