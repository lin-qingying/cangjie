package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.DiagnosticParameterRenderer
import org.cangnova.cangjie.cfir.diagnostics.rendering.toDeprecationWarningMessage

/**
 * 诊断工厂到消息渲染器的注册表。
 */
class CjDiagnosticFactoryToRendererMap internal constructor(
    /**
     * 当前渲染器映射的名称，用于错误消息定位。
     */
    val name: String,
) {
    /**
     * 诊断工厂到渲染器实例的实际映射。
     */
    private val renderersMap: MutableMap<AbstractCjDiagnosticFactory, CjDiagnosticRenderer> = mutableMapOf()

    /**
     * 查询指定诊断工厂对应的渲染器。
     */
    operator fun get(factory: AbstractCjDiagnosticFactory): CjDiagnosticRenderer? = renderersMap[factory]

    /**
     * 当前映射中已注册的诊断工厂集合。
     */
    val factories: Collection<AbstractCjDiagnosticFactory>
        get() = renderersMap.keys

    /**
     * 判断指定诊断工厂是否已有渲染器。
     */
    fun containsKey(factory: AbstractCjDiagnosticFactory): Boolean {
        return renderersMap.containsKey(factory)
    }

    /**
     * 注册无源码诊断工厂的渲染器。
     */
    fun put(factory: CjSourcelessDiagnosticFactory, message: String) {
        put(factory, CjSourcelessDiagnosticRenderer(message))
    }

    /**
     * 注册无参数诊断工厂的渲染器。
     */
    fun put(factory: CjDiagnosticFactory0, message: String) {
        put(factory, SimpleCjDiagnosticRenderer(message))
    }

    /**
     * 注册一参数诊断工厂的渲染器。
     */
    fun <A> put(
        factory: CjDiagnosticFactory1<A>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?
    ) {
        put(factory, CjDiagnosticWithParameters1Renderer(message, rendererA))
    }

    /**
     * 注册二参数诊断工厂的渲染器。
     */
    fun <A, B> put(
        factory: CjDiagnosticFactory2<A, B>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?
    ) {
        put(factory, CjDiagnosticWithParameters2Renderer(message, rendererA, rendererB))
    }

    /**
     * 注册三参数诊断工厂的渲染器。
     */
    fun <A, B, C> put(
        factory: CjDiagnosticFactory3<A, B, C>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?,
        rendererC: DiagnosticParameterRenderer<C>?
    ) {
        put(factory, CjDiagnosticWithParameters3Renderer(message, rendererA, rendererB, rendererC))
    }

    /**
     * 注册四参数诊断工厂的渲染器。
     */
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

    /**
     * 注册无参数退化诊断的 error 和 warning 渲染器。
     */
    fun put(factory: CjDiagnosticFactoryForDeprecation0, message: String) {
        put(factory.errorFactory, SimpleCjDiagnosticRenderer(message))
        put(factory.warningFactory, SimpleCjDiagnosticRenderer(factory.warningMessage(message)))
    }

    /**
     * 注册一参数退化诊断的 error 和 warning 渲染器。
     */
    fun <A> put(
        factory: CjDiagnosticFactoryForDeprecation1<A>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?
    ) {
        put(factory.errorFactory, CjDiagnosticWithParameters1Renderer(message, rendererA))
        put(factory.warningFactory, CjDiagnosticWithParameters1Renderer(factory.warningMessage(message), rendererA))
    }


    /**
     * 注册二参数退化诊断的 error 和 warning 渲染器。
     */
    fun <A, B> put(
        factory: CjDiagnosticFactoryForDeprecation2<A, B>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?
    ) {
        put(factory.errorFactory, CjDiagnosticWithParameters2Renderer(message, rendererA, rendererB))
        put(factory.warningFactory, CjDiagnosticWithParameters2Renderer(factory.warningMessage(message), rendererA, rendererB))
    }

    /**
     * 注册三参数退化诊断的 error 和 warning 渲染器。
     */
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

    /**
     * 注册四参数退化诊断的 error 和 warning 渲染器。
     */
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

    /**
     * 注册渲染器并拒绝重复初始化同一诊断工厂。
     */
    private fun put(factory: AbstractCjDiagnosticFactory, renderer: CjDiagnosticRenderer) {
        if (renderersMap.containsKey(factory)) {
            throw IllegalStateException("Diagnostic renderer is already initialized for $factory")
        }
        renderersMap[factory] = renderer
    }

    /**
     * 将错误级别消息转换为退化 warning 消息。
     */
    private fun CjDiagnosticFactoryForDeprecation<*>.warningMessage(errorMessage: String): String {
        val deprecatingFeature = deprecatingFeature
        return errorMessage.toDeprecationWarningMessage(deprecatingFeature)
    }
}

/**
 * 创建延迟初始化的诊断渲染器映射。
 */
fun CjDiagnosticFactoryToRendererMap(
    name: String,
    init: (CjDiagnosticFactoryToRendererMap) -> Unit,
): Lazy<CjDiagnosticFactoryToRendererMap> {
    return lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CjDiagnosticFactoryToRendererMap(name).also(init)
    }
}

