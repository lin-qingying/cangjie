package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.DiagnosticParameterRenderer
import org.cangnova.cangjie.cfir.diagnostics.rendering.RenderingContext
import org.cangnova.cangjie.cfir.diagnostics.rendering.renderParameter
import java.text.MessageFormat

sealed class CjDiagnosticRenderer {
    abstract val message: String
    abstract fun render(diagnostic: CjDiagnostic): String
    abstract fun renderParameters(diagnostic: CjDiagnostic): Array<out Any?>
}

class SimpleCjDiagnosticRenderer(override val message: String) : CjDiagnosticRenderer() {
    override fun render(diagnostic: CjDiagnostic): String {
        require(diagnostic is CjSimpleDiagnostic)
        return message
    }

    override fun renderParameters(diagnostic: CjDiagnostic): Array<out Any?> {
        require(diagnostic is CjSimpleDiagnostic)
        return emptyArray()
    }
}

sealed class AbstractCjDiagnosticWithParametersRenderer(
    final override val message: String
) : CjDiagnosticRenderer() {
    private val messageFormat = MessageFormat(message)

    final override fun render(diagnostic: CjDiagnostic): String {
        return messageFormat.format(renderParameters(diagnostic))
    }
}

class CjSourcelessDiagnosticRenderer(message: String) : AbstractCjDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: CjDiagnostic): Array<out Any?> {
        require(diagnostic is CjDiagnosticWithoutSource)
        return arrayOf(diagnostic.message)
    }
}

class CjDiagnosticWithParameters1Renderer<A>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
) : AbstractCjDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: CjDiagnostic): Array<out Any?> {
        require(diagnostic is CjDiagnosticWithParameters1<*>)
        val context = RenderingContext.of(diagnostic.context, diagnostic.a)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(renderParameter(diagnostic.a as A, rendererForA, context))
    }
}

class CjDiagnosticWithParameters2Renderer<A, B>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
) : AbstractCjDiagnosticWithParametersRenderer(message) {
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

class CjDiagnosticWithParameters3Renderer<A, B, C>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?,
) : AbstractCjDiagnosticWithParametersRenderer(message) {
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

class CjDiagnosticWithParameters4Renderer<A, B, C, D>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?,
    private val rendererForD: DiagnosticParameterRenderer<D>?,
) : AbstractCjDiagnosticWithParametersRenderer(message) {
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


