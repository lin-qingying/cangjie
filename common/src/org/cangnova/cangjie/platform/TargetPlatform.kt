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
    val componentPlatforms: Set<SimplePlatform>,
) : Iterable<SimplePlatform> by componentPlatforms {
    init {
        require(componentPlatforms.isNotEmpty()) {
            "Don't instantiate TargetPlatform with empty set of platforms"
        }
    }

    val size: Int
        get() = componentPlatforms.size

    override fun toString(): String = presentableDescription

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TargetPlatform) return false
        return componentPlatforms == other.componentPlatforms
    }

    override fun hashCode(): Int = componentPlatforms.hashCode()
}

/**
 * 平台 API 的简单平台抽象，表示一个不可再拆分的单一目标。
 *
 * 对齐 Kotlin `org.jetbrains.kotlin.platform.SimplePlatform`。
 */
abstract class SimplePlatform(
    val platformName: String,
) {
    override fun toString(): String {
        val targetName = targetName
        return if (targetName.isNotEmpty()) "$platformName ($targetName)" else platformName
    }

    /**
     * 目标名称用于序列化与调试输出。
     */
    open val targetName: String
        get() = targetPlatformVersion.description

    abstract val oldFashionedDescription: String

    open val targetPlatformVersion: TargetPlatformVersion = TargetPlatformVersion.NoVersion
}

interface TargetPlatformVersion {
    val description: String

    object NoVersion : TargetPlatformVersion {
        override val description: String = ""
    }
}

fun TargetPlatform?.isMultiPlatform(): Boolean {
    contract { returns(true) implies (this@isMultiPlatform != null) }
    return this != null && size > 1
}

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

fun SimplePlatform.toTargetPlatform(): TargetPlatform = TargetPlatform(setOf(this))

fun SimplePlatform.serializeToString(): String = "$platformName [$targetName]"
