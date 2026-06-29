@file:OptIn(ExperimentalContracts::class)

package org.cangnova.cangjie.platform

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 平台 API 的核心抽象，表示一个由若干简单平台组成的目标平台集合。
 *
 * 对齐 Kotlin `org.jetbrains.kotlin.platform.TargetPlatform`，作为前端与 Analysis API
 * 层承载“编译目标身份”的统一抽象。当前仓颉只需要显式区分 `cjnative` 与 `cjvm`，
 * 但保留集合形态，避免后续扩展时再次重构框架入口。
 */
open class TargetPlatform(
    /**
     * 构成该目标平台的简单平台集合。
     */
    val componentPlatforms: Set<SimplePlatform>,
) : Iterable<SimplePlatform> by componentPlatforms {
    init {
        require(componentPlatforms.isNotEmpty()) {
            "Don't instantiate TargetPlatform with empty set of platforms"
        }
    }

    /**
     * 简单平台数量。
     */
    val size: Int
        get() = componentPlatforms.size

    /**
     * 返回平台集合的人类可读描述。
     */
    override fun toString(): String = presentableDescription

    /**
     * 按简单平台集合判断平台相等。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TargetPlatform) return false
        return componentPlatforms == other.componentPlatforms
    }

    /**
     * 返回简单平台集合的哈希值。
     */
    override fun hashCode(): Int = componentPlatforms.hashCode()
}

/**
 * 平台 API 的简单平台抽象，表示一个不可再拆分的单一目标。
 *
 * 对齐 Kotlin `org.jetbrains.kotlin.platform.SimplePlatform`。
 */
abstract class SimplePlatform(
    /**
     * 平台族名称。
     */
    val platformName: String,
) {
    /**
     * 返回包含版本目标名的平台展示文本。
     */
    override fun toString(): String {
        val targetName = targetName
        return if (targetName.isNotEmpty()) "$platformName ($targetName)" else platformName
    }

    /**
     * 目标名称用于序列化与调试输出。
     */
    open val targetName: String
        get() = targetPlatformVersion.description

    /**
     * 旧式平台描述文本。
     */
    abstract val oldFashionedDescription: String

    /**
     * 目标平台版本。
     */
    open val targetPlatformVersion: TargetPlatformVersion = TargetPlatformVersion.NoVersion
}

/**
 * 目标平台版本描述。
 */
interface TargetPlatformVersion {
    /**
     * 版本描述文本。
     */
    val description: String

    /**
     * 没有显式版本的平台版本。
     */
    object NoVersion : TargetPlatformVersion {
        /**
         * 无版本时描述为空字符串。
         */
        override val description: String = ""
    }
}

/**
 * 判断目标平台是否由多个简单平台组成。
 */
fun TargetPlatform?.isMultiPlatform(): Boolean {
    contract { returns(true) implies (this@isMultiPlatform != null) }
    return this != null && size > 1
}

/**
 * 判断目标平台是否表示 common 平台集合。
 */
fun TargetPlatform?.isCommon(): Boolean {
    contract { returns(true) implies (this@isCommon != null) }
    return isMultiPlatform() && iterator().let { iterator ->
        val firstPlatformName = iterator.next().platformName
        while (iterator.hasNext()) {
            if (iterator.next().platformName != firstPlatformName) return@let true
        }
        false
    }
}

/**
 * 将简单平台包装为只包含自身的目标平台。
 */
fun SimplePlatform.toTargetPlatform(): TargetPlatform = TargetPlatform(setOf(this))

/**
 * 将简单平台序列化为稳定字符串。
 */
fun SimplePlatform.serializeToString(): String = "$platformName [$targetName]"
