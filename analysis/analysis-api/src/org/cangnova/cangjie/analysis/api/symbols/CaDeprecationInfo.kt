package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.resolve.deprecation.DeprecationLevelValue

/**
 * Analysis API 对声明弃用状态的公开视图，对位 Kotlin 编译器 `DeprecationInfo`。
 */
abstract class CaDeprecationInfo : Comparable<CaDeprecationInfo> {
    /** 弃用严重级别。 */
    abstract val deprecationLevel: DeprecationLevelValue

    /** 当前弃用是否传播到 override。 */
    abstract val propagatesToOverrides: Boolean

    /** 弃用消息；读取时机由具体前端实现保证。 */
    abstract val message: String?

    override fun compareTo(other: CaDeprecationInfo): Int {
        val levelResult = deprecationLevel.compareTo(other.deprecationLevel)
        return if (levelResult == 0 && !propagatesToOverrides && other.propagatesToOverrides) {
            1
        } else {
            levelResult
        }
    }
}

/**
 * 由当前分析会话已解析的弃用信息创建的不可变状态值。
 */
data class CaSimpleDeprecationInfo(
    override val deprecationLevel: DeprecationLevelValue,
    override val propagatesToOverrides: Boolean,
    override val message: String?,
) : CaDeprecationInfo()
