@file:Suppress("UNCHECKED_CAST")

package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.WarningLevel
import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import kotlin.reflect.KClass

/**
 * 标记诊断工厂底层创建方法的 opt-in 注解。
 *
 * 普通调用方应优先通过 [DiagnosticReporter.reportOn] 进入诊断上报管线。
 */
@RequiresOptIn("Please use DiagnosticReporter.reportOn method if possible")
annotation class InternalDiagnosticFactoryMethod

/**
 * 仓颉诊断工厂的公共基类。
 */
sealed class AbstractCjDiagnosticFactory(
    /**
     * 诊断工厂的稳定名称。
     */
    val name: String,
    /**
     * 诊断默认严重级别。
     */
    val severity: Severity,
    /**
     * 当前工厂所属的诊断渲染器集合。
     */
    val rendererFactory: BaseDiagnosticRendererFactory
) {
    /**
     * 当前工厂对应的仓颉诊断渲染器。
     */
    val cjRenderer: CjDiagnosticRenderer
        get() = rendererFactory.MAP[this]
            ?: error("Renderer is not found for factory $this inside ${rendererFactory.MAP.name} renderer map")

    /**
     * 根据语言版本设置和 warning level 覆盖计算本次诊断的有效严重级别。
     */
    fun getEffectiveSeverity(languageVersionSettings: LanguageVersionSettings): Severity? {
        return when (languageVersionSettings.getFlag(AnalysisFlags.warningLevels)[name]) {
            WarningLevel.Error -> Severity.ERROR
            WarningLevel.Warning -> Severity.FIXED_WARNING
            WarningLevel.Disabled -> null
            null -> severity
        }
    }

    /**
     * 返回诊断工厂名称，便于调试和生成诊断定义文本。
     */
    override fun toString(): String {
        return name
    }
}

/**
 * 不绑定源码元素的诊断工厂。
 */
class CjSourcelessDiagnosticFactory(
    name: String,
    severity: Severity,
    rendererFactory: BaseDiagnosticRendererFactory,
) : AbstractCjDiagnosticFactory(name, severity, rendererFactory) {
    /**
     * 创建无源码诊断；若当前配置禁用了该诊断则返回 null。
     */
    fun create(message: String, location: CompilerMessageSourceLocation?, context: DiagnosticBaseContext): CjDiagnosticWithoutSource? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return CjDiagnosticWithoutSource(message, location, effectiveSeverity, this, context)
    }
}

/**
 * 绑定源码元素的参数化诊断工厂基类。
 */
sealed class CjDiagnosticFactoryN(
    name: String,
    severity: Severity,
    /**
     * 未显式指定定位策略时使用的默认定位策略。
     */
    val defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 该诊断工厂允许锚定的 PSI 类型。
     */
    val psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory
) : AbstractCjDiagnosticFactory(name, severity, rendererFactory)

/**
 * 无参数诊断工厂。
 */
class CjDiagnosticFactory0(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
    /**
     * 在指定源码元素上创建无参数诊断。
     */
    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractCjSourceElement,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        context: DiagnosticBaseContext,
    ): CjSimpleDiagnostic? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return when (element) {
            is CjPsiSourceElement -> CjPsiSimpleDiagnostic(
                element,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
            is CjLightSourceElement -> CjLightSimpleDiagnostic(
                element,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
            else -> CjOffsetsOnlySimpleDiagnostic(
                element,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
        }
    }
}

/**
 * 一参数诊断工厂。
 */
class CjDiagnosticFactory1<A>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
    /**
     * 在指定源码元素上创建一参数诊断。
     */
    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractCjSourceElement,
        a: A,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        context: DiagnosticBaseContext,
    ): CjDiagnosticWithParameters1<A>? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return when (element) {
            is CjPsiSourceElement -> CjPsiDiagnosticWithParameters1(
                element,
                a,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
            is CjLightSourceElement -> CjLightDiagnosticWithParameters1(
                element,
                a,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
            else -> CjOffsetsOnlyDiagnosticWithParameters1(
                element,
                a,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
        }
    }
}

/**
 * 二参数诊断工厂。
 */
class CjDiagnosticFactory2<A, B>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
    /**
     * 在指定源码元素上创建二参数诊断。
     */
    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractCjSourceElement,
        a: A,
        b: B,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        context: DiagnosticBaseContext,
    ): CjDiagnosticWithParameters2<A, B>? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return when (element) {
            is CjPsiSourceElement -> CjPsiDiagnosticWithParameters2(
                element,
                a,
                b,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
            is CjLightSourceElement -> CjLightDiagnosticWithParameters2(
                element,
                a,
                b,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
            else -> CjOffsetsOnlyDiagnosticWithParameters2(
                element,
                a,
                b,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
        }
    }
}

/**
 * 三参数诊断工厂。
 */
class CjDiagnosticFactory3<A, B, C>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
    /**
     * 在指定源码元素上创建三参数诊断。
     */
    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractCjSourceElement,
        a: A,
        b: B,
        c: C,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        context: DiagnosticBaseContext,
    ): CjDiagnosticWithParameters3<A, B, C>? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return when (element) {
            is CjPsiSourceElement -> CjPsiDiagnosticWithParameters3(
                element,
                a,
                b,
                c,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
            is CjLightSourceElement -> CjLightDiagnosticWithParameters3(
                element,
                a,
                b,
                c,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
            else -> CjOffsetsOnlyDiagnosticWithParameters3(
                element,
                a,
                b,
                c,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
        }
    }
}

/**
 * 四参数诊断工厂。
 */
class CjDiagnosticFactory4<A, B, C, D>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
    /**
     * 在指定源码元素上创建四参数诊断。
     */
    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractCjSourceElement,
        a: A,
        b: B,
        c: C,
        d: D,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        context: DiagnosticBaseContext,
    ): CjDiagnosticWithParameters4<A, B, C, D>? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return when (element) {
            is CjPsiSourceElement -> CjPsiDiagnosticWithParameters4(
                element,
                a,
                b,
                c,
                d,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
            is CjLightSourceElement -> CjLightDiagnosticWithParameters4(
                element,
                a,
                b,
                c,
                d,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
            else -> CjOffsetsOnlyDiagnosticWithParameters4(
                element,
                a,
                b,
                c,
                d,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy,
                context,
            )
        }
    }
}

// ------------------------------ factories for deprecation ------------------------------

/**
 * 同一语言特性在 warning/error 两种级别下的诊断工厂组合。
 */
sealed class CjDiagnosticFactoryForDeprecation<F : CjDiagnosticFactoryN>(
    /**
     * 退化特性诊断的基础名称。
     */
    val name: String,
    /**
     * 该特性达到错误级别时对应的语言特性开关。
     */
    val deprecatingFeature: LanguageFeature,
    /**
     * 该特性仍处于警告阶段时使用的诊断工厂。
     */
    val warningFactory: F,
    /**
     * 该特性升级为错误时使用的诊断工厂。
     */
    val errorFactory: F
)

/**
 * 退化诊断 warning 工厂名称后缀。
 */
private const val WARNING = "_WARNING"
/**
 * 退化诊断 error 工厂名称后缀。
 */
private const val ERROR = "_ERROR"

/**
 * 无参数退化诊断工厂组合。
 */
class CjDiagnosticFactoryForDeprecation0(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryForDeprecation<CjDiagnosticFactory0>(
    name,
    featureForError,
    CjDiagnosticFactory0("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType, rendererFactory),
    CjDiagnosticFactory0("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType, rendererFactory)
)

/**
 * 一参数退化诊断工厂组合。
 */
class CjDiagnosticFactoryForDeprecation1<A>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryForDeprecation<CjDiagnosticFactory1<A>>(
    name,
    featureForError,
    CjDiagnosticFactory1("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType, rendererFactory),
    CjDiagnosticFactory1("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType, rendererFactory)
)

/**
 * 二参数退化诊断工厂组合。
 */
class CjDiagnosticFactoryForDeprecation2<A, B>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryForDeprecation<CjDiagnosticFactory2<A, B>>(
    name,
    featureForError,
    CjDiagnosticFactory2("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType, rendererFactory),
    CjDiagnosticFactory2("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType, rendererFactory)
)

/**
 * 三参数退化诊断工厂组合。
 */
class CjDiagnosticFactoryForDeprecation3<A, B, C>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryForDeprecation<CjDiagnosticFactory3<A, B, C>>(
    name,
    featureForError,
    CjDiagnosticFactory3("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType, rendererFactory),
    CjDiagnosticFactory3("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType, rendererFactory)
)

/**
 * 四参数退化诊断工厂组合。
 */
class CjDiagnosticFactoryForDeprecation4<A, B, C, D>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryForDeprecation<CjDiagnosticFactory4<A, B, C, D>>(
    name,
    featureForError,
    CjDiagnosticFactory4("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType, rendererFactory),
    CjDiagnosticFactory4("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType, rendererFactory)
)


