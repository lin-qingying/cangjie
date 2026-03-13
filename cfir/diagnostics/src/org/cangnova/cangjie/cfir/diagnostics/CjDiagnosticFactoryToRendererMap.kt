package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.DiagnosticParameterRenderer
import org.cangnova.cangjie.cfir.diagnostics.rendering.toDeprecationWarningMessage

class CjDiagnosticFactoryToRendererMap internal constructor(val name: String) {
    private val renderersMap: MutableMap<AbstractCjDiagnosticFactory, CjDiagnosticRenderer> = mutableMapOf()

    operator fun get(factory: AbstractCjDiagnosticFactory): CjDiagnosticRenderer? = renderersMap[factory]

    val factories: Collection<AbstractCjDiagnosticFactory>
        get() = renderersMap.keys

    fun containsKey(factory: AbstractCjDiagnosticFactory): Boolean {
        return renderersMap.containsKey(factory)
    }

    fun put(factory: CjSourcelessDiagnosticFactory, message: String) {
        put(factory, CjSourcelessDiagnosticRenderer(message))
    }

    fun put(factory: CjDiagnosticFactory0, message: String) {
        put(factory, SimpleCjDiagnosticRenderer(message))
    }

    fun <A> put(
        factory: CjDiagnosticFactory1<A>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?
    ) {
        put(factory, CjDiagnosticWithParameters1Renderer(message, rendererA))
    }

    fun <A, B> put(
        factory: CjDiagnosticFactory2<A, B>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?
    ) {
        put(factory, CjDiagnosticWithParameters2Renderer(message, rendererA, rendererB))
    }

    fun <A, B, C> put(
        factory: CjDiagnosticFactory3<A, B, C>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?,
        rendererC: DiagnosticParameterRenderer<C>?
    ) {
        put(factory, CjDiagnosticWithParameters3Renderer(message, rendererA, rendererB, rendererC))
    }

    fun <A, B, C, D> put(
        factory: CjDiagnosticFactory4<A, B, C, D>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?,
        rendererC: DiagnosticParameterRenderer<C>?,
        rendererD: DiagnosticParameterRenderer<D>?
    ) {
        put(factory, CjDiagnosticWithParameters4Renderer(message, rendererA, rendererB, rendererC, rendererD))
    }

    fun put(factory: CjDiagnosticFactoryForDeprecation0, message: String) {
        put(factory.errorFactory, SimpleCjDiagnosticRenderer(message))
        put(factory.warningFactory, SimpleCjDiagnosticRenderer(factory.warningMessage(message)))
    }

    fun <A> put(
        factory: CjDiagnosticFactoryForDeprecation1<A>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?
    ) {
        put(factory.errorFactory, CjDiagnosticWithParameters1Renderer(message, rendererA))
        put(factory.warningFactory, CjDiagnosticWithParameters1Renderer(factory.warningMessage(message), rendererA))
    }

    fun <A, B> put(
        factory: CjDiagnosticFactoryForDeprecation2<A, B>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?
    ) {
        put(factory.errorFactory, CjDiagnosticWithParameters2Renderer(message, rendererA, rendererB))
        put(factory.warningFactory, CjDiagnosticWithParameters2Renderer(factory.warningMessage(message), rendererA, rendererB))
    }

    fun <A, B, C> put(
        factory: CjDiagnosticFactoryForDeprecation3<A, B, C>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?,
        rendererC: DiagnosticParameterRenderer<C>?
    ) {
        put(factory.errorFactory, CjDiagnosticWithParameters3Renderer(message, rendererA, rendererB, rendererC))
        put(factory.warningFactory, CjDiagnosticWithParameters3Renderer(factory.warningMessage(message), rendererA, rendererB, rendererC))
    }

    fun <A, B, C, D> put(
        factory: CjDiagnosticFactoryForDeprecation4<A, B, C, D>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?,
        rendererC: DiagnosticParameterRenderer<C>?,
        rendererD: DiagnosticParameterRenderer<D>?
    ) {
        put(factory.errorFactory, CjDiagnosticWithParameters4Renderer(message, rendererA, rendererB, rendererC, rendererD))
        put(factory.warningFactory, CjDiagnosticWithParameters4Renderer(factory.warningMessage(message), rendererA, rendererB, rendererC, rendererD))
    }

    private fun put(factory: AbstractCjDiagnosticFactory, renderer: CjDiagnosticRenderer) {
        if (renderersMap.containsKey(factory)) {
            throw IllegalStateException("Diagnostic renderer is already initialized for $factory")
        }
        renderersMap[factory] = renderer
    }

    private fun CjDiagnosticFactoryForDeprecation<*>.warningMessage(errorMessage: String): String {
        val deprecatingFeature = deprecatingFeature
        return errorMessage.toDeprecationWarningMessage(deprecatingFeature)
    }
}

fun CjDiagnosticFactoryToRendererMap(
    name: String,
    init: (CjDiagnosticFactoryToRendererMap) -> Unit,
): Lazy<CjDiagnosticFactoryToRendererMap> {
    return lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CjDiagnosticFactoryToRendererMap(name).also(init)
    }
}


