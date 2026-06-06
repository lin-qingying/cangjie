package org.cangnova.cangjie.platform

/**
 * 仓颉语言前端当前识别的简单目标平台。
 *
 * 这里只表达“高层编译目标身份”，不表达具体 native OS/ABI 细节；native 细节继续
 * 由 CFIR / backend 现有的专有平台模型承载。
 */
sealed class CangJieSimplePlatform(
    private val targetId: String,
) : SimplePlatform("CangJie") {
    override val oldFashionedDescription: String
        get() = "CangJie $targetId "

    override val targetName: String
        get() = targetId
}

object CjNativeSimplePlatform : CangJieSimplePlatform("cjnative")

object CjvmSimplePlatform : CangJieSimplePlatform("cjvm")

/**
 * 仓颉目标平台入口。对齐 Kotlin 的 `JvmPlatforms` / `NativePlatforms` 这类工厂对象。
 */
object CangJiePlatforms {
    val cjNative: TargetPlatform = CjNativeSimplePlatform.toTargetPlatform()

    val cjvm: TargetPlatform = CjvmSimplePlatform.toTargetPlatform()

    /**
     * 当前仓颉前端的默认目标仍然是 `cjnative`。
     */
    val defaultCangJiePlatform: TargetPlatform
        get() = cjNative

    val allDefaultTargetPlatforms: List<TargetPlatform>
        get() = listOf(cjNative, cjvm)
}

fun TargetPlatform?.isCjNative(): Boolean = this?.singleOrNull() === CjNativeSimplePlatform

fun TargetPlatform?.isCjvm(): Boolean = this?.singleOrNull() === CjvmSimplePlatform
