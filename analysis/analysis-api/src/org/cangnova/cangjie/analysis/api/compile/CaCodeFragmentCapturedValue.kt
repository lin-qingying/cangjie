package org.cangnova.cangjie.analysis.api.compile

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 代码片段(code fragment)分析时捕获的值。
 *
 * 用于在调试器、表达式求值等场景把外层作用域的变量、`this`、`super` 等
 * 显式表示为一组可被序列化的捕获项;
 * 与 Kotlin Analysis API 的 `KaCodeFragmentCapturedValue` 对齐。
 *
 * - [name] 用于显示和符号引用;
 * - [isMutated] 标识值在片段中是否被写;
 * - [isCrossingInlineBounds] 标识捕获是否跨越内联边界;
 * - [depthRelativeToCurrentFrame] 表示相对当前栈帧的嵌套层数。
 */
@CaExperimentalApi
sealed class CaCodeFragmentCapturedValue(
    /**
     * 捕获值在代码片段中的展示名或引用名。
     */
    val name: String,

    /**
     * 捕获值是否会在代码片段执行期间被写入。
     */
    val isMutated: Boolean,

    /**
     * 捕获是否跨越 inline 调用边界。
     */
    val isCrossingInlineBounds: Boolean,

    /**
     * 捕获值相对当前调试栈帧的层级深度。
     */
    val depthRelativeToCurrentFrame: Int,
) {
    /** 用于在 UI/求值器中展示捕获值的文本形式。 */
    open val displayText: String
        get() = name

    /**
     * 返回包含捕获值名称、变更状态和展示文本的调试字符串。
     */
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
        /**
         * 被捕获 `this` 所属的类标识。
         */
        private val classId: ClassId,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int,
    ) : CaCodeFragmentCapturedValue("<this>", isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame) {
        /**
         * 返回 `this` 或带类名限定的 `this@Class` 展示文本。
         */
        override val displayText: String
            get() {
                val simpleName = classId.shortClassName
                return if (simpleName.isSpecial) "this" else "this@" + simpleName.asString()
            }
    }

    /** 表示 `super.foo()` 形式捕获到的父类接收者。 */
    @CaExperimentalApi
    class SuperClass(
        /**
         * 被捕获 `super` 接收者对应的父类标识。
         */
        private val classId: ClassId,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int,
    ) : CaCodeFragmentCapturedValue("<super>", isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame) {
        /**
         * 返回 `super@Class` 展示文本。
         */
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
