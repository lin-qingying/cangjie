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
    @JvmStatic
    fun comparePublicNonFinalFieldsWithSkip(first: Any, second: Any): Boolean {
        return comparePublicNonFinalFields(
            first,
            second,
            Predicate { field -> field?.getAnnotation<SkipInEquals>(SkipInEquals::class.java) == null },
        )
    }

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

    private fun isPublic(field: Field): Boolean = (field.modifiers and Modifier.PUBLIC) != 0

    private fun isFinal(field: Field): Boolean = (field.modifiers and Modifier.FINAL) != 0

    @Retention(AnnotationRetention.RUNTIME)
    annotation class SkipInEquals
}
