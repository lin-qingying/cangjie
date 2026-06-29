package org.cangnova.cangjie.deveco.ui

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

/** DevEco 消息资源 bundle 路径。 */
private const val BUNDLE = "messages.DevEcoMessageBundle"

/**
 * DevEco UI 使用的本地化消息访问入口。
 */
internal object DevEcoMessageBundle {
    /** IntelliJ 动态消息 bundle 实例。 */
    private val instance = DynamicBundle(DevEcoMessageBundle::class.java, BUNDLE)

    /**
     * 立即解析指定 key 的本地化消息。
     */
    @JvmStatic
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any?): @Nls String {
        return instance.getMessage(key, *params)
    }

    /**
     * 创建延迟解析的本地化消息 supplier。
     */
    @JvmStatic
    fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?): Supplier<@Nls String> {
        return instance.getLazyMessage(key, *params)
    }
}
