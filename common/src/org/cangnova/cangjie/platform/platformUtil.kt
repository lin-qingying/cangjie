package org.cangnova.cangjie.platform

import kotlin.reflect.KClass

/**
 * 返回目标平台中指定类型的简单平台列表。
 */
inline fun <reified T : SimplePlatform> TargetPlatform.subplatformsOfType(): List<T> =
    componentPlatforms.filterIsInstance<T>()

/**
 * 返回目标平台中指定 Java class 类型的简单平台列表。
 */
fun <T> TargetPlatform.subplatformsOfType(klass: Class<T>): List<T> = componentPlatforms.filterIsInstance(klass)

/**
 * 判断可空目标平台是否包含指定简单平台类型。
 */
inline fun <reified T : SimplePlatform> TargetPlatform?.has(): Boolean =
    this != null && subplatformsOfType<T>().isNotEmpty()

/**
 * 判断可空目标平台是否包含指定 KClass 对应的简单平台类型。
 */
fun TargetPlatform?.has(klass: KClass<*>): Boolean = this != null && subplatformsOfType(klass.java).isNotEmpty()

/**
 * 兼容旧平台字符串表示。
 *
 * 这里保留 Kotlin 同名扩展属性的职责：旧序列化或缓存如果依赖历史平台描述字符串，
 * 仍然可以继续拿到稳定值；新代码应优先使用 [presentableDescription]。
 */
val TargetPlatform.oldFashionedDescription: String
    get() = this.singleOrNull()?.oldFashionedDescription ?: "Common (experimental) "

/**
 * 面向展示的新平台描述。
 *
 * 对齐 Kotlin `TargetPlatform.presentableDescription`，多平台场景使用 `/` 连接各个
 * component platform；当前仓颉前端虽然只显式区分 `cjnative` / `cjvm`，但提前保留
 * 同样的渲染契约，避免后续扩展时再改调用方。
 */
val TargetPlatform.presentableDescription: String
    get() = componentPlatforms.joinToString(separator = "/")
