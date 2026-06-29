package org.cangnova.cangjie.formatter

import com.intellij.openapi.util.Comparing
import com.intellij.util.containers.ContainerUtil
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.function.Predicate

/**
 * formatter 自身使用的反射工具。
 *
 * 共享 formatting 模块不能反向依赖 IDE foundation，因此把 code-style settings
 * 需要的字段比较能力直接下沉到这里。
 */
object ReflectionUtil {
    /**
     * 比较两个对象公开且非 final 的字段，跳过带 [SkipInEquals] 的字段。
     */
    @JvmStatic
    fun comparePublicNonFinalFieldsWithSkip(first: Any, second: Any): Boolean {
        return comparePublicNonFinalFields(
            first,
            second,
            Predicate { field -> field?.getAnnotation<SkipInEquals>(SkipInEquals::class.java) == null },
        )
    }

    /**
     * 按过滤谓词比较两个对象共有的公开非 final 字段。
     */
    private fun comparePublicNonFinalFields(
        first: Any,
        second: Any,
        acceptPredicate: Predicate<Field?>?,
    ): Boolean {
        val firstFields = ContainerUtil.newHashSet<Field?>(*first.javaClass.getFields())

        for (field in second.javaClass.getFields()) {
            if (field !in firstFields) continue
            if (!isPublic(field) || isFinal(field) || acceptPredicate?.test(field) == false) continue

            try {
                if (!Comparing.equal(field.get(first), field.get(second))) {
                    return false
                }
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }
        }

        return true
    }

    /**
     * 判断字段是否为 public。
     */
    private fun isPublic(field: Field): Boolean = (field.modifiers and Modifier.PUBLIC) != 0

    /**
     * 判断字段是否为 final。
     */
    private fun isFinal(field: Field): Boolean = (field.modifiers and Modifier.FINAL) != 0

    /**
     * 标记字段不参与 formatter settings 的 equals 比较。
     */
    @Retention(AnnotationRetention.RUNTIME)
    annotation class SkipInEquals
}
