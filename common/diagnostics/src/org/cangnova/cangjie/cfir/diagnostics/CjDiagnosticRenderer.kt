package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.DiagnosticParameterRenderer
import org.cangnova.cangjie.cfir.diagnostics.rendering.RenderingContext
import org.cangnova.cangjie.cfir.diagnostics.rendering.renderParameter
import java.text.MessageFormat

/**
 * 仓颉诊断消息渲染器基类。
 */
sealed class CjDiagnosticRenderer {
    /**
     * 诊断消息模板。
     */
    abstract val message: String
    /**
     * 将诊断对象渲染为最终消息文本。
     */
    abstract fun render(diagnostic: CjDiagnostic): String
    /**
     * 提取并渲染诊断模板参数。
     */
    abstract fun renderParameters(diagnostic: CjDiagnostic): Array<out Any?>
}

/**
 * 无参数诊断渲染器，直接返回固定消息。
 */
class SimpleCjDiagnosticRenderer(override val message: String) : CjDiagnosticRenderer() {
    /**
     * 渲染无参数诊断消息。
     */
    override fun render(diagnostic: CjDiagnostic): String {
        require(diagnostic is CjSimpleDiagnostic)
        return message
    }

    /**
     * 无参数诊断没有模板参数。
     */
    override fun renderParameters(diagnostic: CjDiagnostic): Array<out Any?> {
        require(diagnostic is CjSimpleDiagnostic)
        return emptyArray()
    }
}

/**
 * 基于 [MessageFormat] 的参数化诊断渲染器基类。
 */
sealed class AbstractCjDiagnosticWithParametersRenderer(
    /**
     * MessageFormat 使用的诊断消息模板。
     */
    final override val message: String
) : CjDiagnosticRenderer() {
    /**
     * 已编译的消息格式化器。
     */
    private val messageFormat = MessageFormat(message)

    /**
     * 使用已渲染参数格式化最终诊断消息。
     */
    final override fun render(diagnostic: CjDiagnostic): String {
        return messageFormat.format(renderParameters(diagnostic))
    }
}

/**
 * 无源码诊断渲染器，将预生成消息作为模板参数。
 */
class CjSourcelessDiagnosticRenderer(message: String) : AbstractCjDiagnosticWithParametersRenderer(message) {
    /**
     * 返回无源码诊断携带的消息文本。
     */
    override fun renderParameters(diagnostic: CjDiagnostic): Array<out Any?> {
        require(diagnostic is CjDiagnosticWithoutSource)
        return arrayOf(diagnostic.message)
    }
}

/**
 * 一参数诊断渲染器。
 */
class CjDiagnosticWithParameters1Renderer<A>(
    message: String,
    /**
     * 第一个诊断参数的专用渲染器。
     */
    private val rendererForA: DiagnosticParameterRenderer<A>?,
) : AbstractCjDiagnosticWithParametersRenderer(message) {
    /**
     * 渲染一参数诊断的模板参数数组。
     */
    override fun renderParameters(diagnostic: CjDiagnostic): Array<out Any?> {
        require(diagnostic is CjDiagnosticWithParameters1<*>)
        val context = RenderingContext.of(diagnostic.context, diagnostic.a)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(renderParameter(diagnostic.a as A, rendererForA, context))
    }
}

/**
 * 二参数诊断渲染器。
 */
class CjDiagnosticWithParameters2Renderer<A, B>(
    message: String,
    /**
     * 第一个诊断参数的专用渲染器。
     */
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    /**
     * 第二个诊断参数的专用渲染器。
     */
    private val rendererForB: DiagnosticParameterRenderer<B>?,
) : AbstractCjDiagnosticWithParametersRenderer(message) {
    /**
     * 渲染二参数诊断的模板参数数组。
     */
    override fun renderParameters(diagnostic: CjDiagnostic): Array<out Any?> {
        require(diagnostic is CjDiagnosticWithParameters2<*, *>)
        val context = RenderingContext.of(diagnostic.context, diagnostic.a, diagnostic.b)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(
            renderParameter(diagnostic.a as A, rendererForA, context),
            renderParameter(diagnostic.b as B, rendererForB, context),
        )
    }
}

/**
 * 三参数诊断渲染器。
 */
class CjDiagnosticWithParameters3Renderer<A, B, C>(
    message: String,
    /**
     * 第一个诊断参数的专用渲染器。
     */
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    /**
     * 第二个诊断参数的专用渲染器。
     */
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    /**
     * 第三个诊断参数的专用渲染器。
     */
    private val rendererForC: DiagnosticParameterRenderer<C>?,
) : AbstractCjDiagnosticWithParametersRenderer(message) {
    /**
     * 渲染三参数诊断的模板参数数组。
     */
    override fun renderParameters(diagnostic: CjDiagnostic): Array<out Any?> {
        require(diagnostic is CjDiagnosticWithParameters3<*, *, *>)
        val context = RenderingContext.of(diagnostic.context, diagnostic.a, diagnostic.b, diagnostic.c)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(
            renderParameter(diagnostic.a as A, rendererForA, context),
            renderParameter(diagnostic.b as B, rendererForB, context),
            renderParameter(diagnostic.c as C, rendererForC, context),
        )
    }
}

/**
 * 四参数诊断渲染器。
 */
class CjDiagnosticWithParameters4Renderer<A, B, C, D>(
    message: String,
    /**
     * 第一个诊断参数的专用渲染器。
     */
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    /**
     * 第二个诊断参数的专用渲染器。
     */
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    /**
     * 第三个诊断参数的专用渲染器。
     */
    private val rendererForC: DiagnosticParameterRenderer<C>?,
    /**
     * 第四个诊断参数的专用渲染器。
     */
    private val rendererForD: DiagnosticParameterRenderer<D>?,
) : AbstractCjDiagnosticWithParametersRenderer(message) {
    /**
     * 渲染四参数诊断的模板参数数组。
     */
    override fun renderParameters(diagnostic: CjDiagnostic): Array<out Any?> {
        require(diagnostic is CjDiagnosticWithParameters4<*, *, *, *>)
        val context = RenderingContext.of(diagnostic.context, diagnostic.a, diagnostic.b, diagnostic.c, diagnostic.d)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(
            renderParameter(diagnostic.a as A, rendererForA, context),
            renderParameter(diagnostic.b as B, rendererForB, context),
            renderParameter(diagnostic.c as C, rendererForC, context),
            renderParameter(diagnostic.d as D, rendererForD, context),
        )
    }
}

