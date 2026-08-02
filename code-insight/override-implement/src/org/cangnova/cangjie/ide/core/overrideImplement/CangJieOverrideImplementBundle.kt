package org.cangnova.cangjie.ide.core.overrideImplement

import org.cangnova.cangjie.messages.AbstractCangJieBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/**
 * override/implement 功能的资源包路径。
 */
@NonNls
private const val BUNDLE = "messages.CangJieOverrideImplementBundle"

/**
 * override/implement 功能使用的本地化消息入口。
 */
object CangJieOverrideImplementBundle : AbstractCangJieBundle(BUNDLE) {
    /**
     * 读取本地化消息。
     */
    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
