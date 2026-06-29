package org.cangnova.cangjie.platform

/**
 * 仓颉语言前端当前识别的简单目标平台。
 *
 * 这里只表达“高层编译目标身份”，不表达具体 native OS/ABI 细节；native 细节继续
 * 由 CFIR / backend 现有的专有平台模型承载。
 */
sealed class CangJieSimplePlatform(
    /**
     * 仓颉目标平台的稳定 ID。
     */
    private val targetId: String,
) : SimplePlatform("CangJie") {
    /**
     * 旧式平台描述。
     */
    override val oldFashionedDescription: String
        get() = "CangJie $targetId "

    /**
     * 平台目标名。
     */
    override val targetName: String
        get() = targetId
}

/**
 * 仓颉 native 简单平台。
 */
object CjNativeSimplePlatform : CangJieSimplePlatform("cjnative")

/**
 * 仓颉 JVM 简单平台。
 */
object CjvmSimplePlatform : CangJieSimplePlatform("cjvm")

/**
 * 仓颉目标平台入口。对齐 Kotlin 的 `JvmPlatforms` / `NativePlatforms` 这类工厂对象。
 */
object CangJiePlatforms {
    /**
     * 仓颉 native 目标平台。
     */
    val cjNative: TargetPlatform = CjNativeSimplePlatform.toTargetPlatform()

    /**
     * 仓颉 JVM 目标平台。
     */
    val cjvm: TargetPlatform = CjvmSimplePlatform.toTargetPlatform()

    /**
     * 当前仓颉前端的默认目标仍然是 `cjnative`。
     */
    val defaultCangJiePlatform: TargetPlatform
        get() = cjNative

    /**
     * 当前默认可枚举的仓颉目标平台列表。
     */
    val allDefaultTargetPlatforms: List<TargetPlatform>
        get() = listOf(cjNative, cjvm)
}

/**
 * 判断目标平台是否为仓颉 native。
 */
fun TargetPlatform?.isCjNative(): Boolean = this?.singleOrNull() === CjNativeSimplePlatform

/**
 * 判断目标平台是否为仓颉 JVM。
 */
fun TargetPlatform?.isCjvm(): Boolean = this?.singleOrNull() === CjvmSimplePlatform
