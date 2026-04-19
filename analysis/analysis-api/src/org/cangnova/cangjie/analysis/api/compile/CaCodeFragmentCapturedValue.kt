package org.cangnova.cangjie.analysis.api.compile

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

@CaExperimentalApi
sealed class CaCodeFragmentCapturedValue(
    val name: String,
    val isMutated: Boolean,
    val isCrossingInlineBounds: Boolean,
    val depthRelativeToCurrentFrame: Int,
) {
    open val displayText: String
        get() = name

    override fun toString(): String {
        return this::class.simpleName + "[name: " + name + "; isMutated: " + isMutated + "; displayText: " + displayText + "]"
    }

    /** 表示局部变量或参数。 */
    @CaExperimentalApi
    class Local(
        name: Name,
        isMutated: Boolean,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int,
    ) : CaCodeFragmentCapturedValue(name.asString(), isMutated, isCrossingInlineBounds, depthRelativeToCurrentFrame)

    /** 表示捕获到的外层类实例。 */
    @CaExperimentalApi
    class ContainingClass(
        private val classId: ClassId,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int,
    ) : CaCodeFragmentCapturedValue("<this>", isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame) {
        override val displayText: String
            get() {
                val simpleName = classId.shortClassName
                return if (simpleName.isSpecial) "this" else "this@" + simpleName.asString()
            }
    }

    /** 表示 `super.foo()` 形式捕获到的父类接收者。 */
    @CaExperimentalApi
    class SuperClass(
        private val classId: ClassId,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int,
    ) : CaCodeFragmentCapturedValue("<super>", isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame) {
        override val displayText: String
            get() = "super@" + classId.shortClassName.asString()
    }

    /** 表示由分析入口额外提供的外部值。 */
    @CaExperimentalApi
    class ForeignValue(
        name: Name,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int,
    ) : CaCodeFragmentCapturedValue(name.asString(), isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame)

    /** 表示 `coroutineContext` 运行时值。 */
    @CaExperimentalApi
    class CoroutineContext(
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int,
    ) : CaCodeFragmentCapturedValue("coroutineContext", isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame)
}
