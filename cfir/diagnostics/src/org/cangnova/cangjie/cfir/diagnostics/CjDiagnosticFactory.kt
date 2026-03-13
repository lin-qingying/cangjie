@file:Suppress("UNCHECKED_CAST")

package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cli.common.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.config.AnalysisFlags
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.config.WarningLevel
import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.source.CjLightSourceElement
import org.cangnova.cangjie.cfir.source.CjPsiSourceElement
import kotlin.reflect.KClass

@RequiresOptIn("Please use DiagnosticReporter.reportOn method if possible")
annotation class InternalDiagnosticFactoryMethod

sealed class AbstractCjDiagnosticFactory(
    val name: String,
    val severity: Severity,
    val rendererFactory: BaseDiagnosticRendererFactory
) {
    val ktRenderer: CjDiagnosticRenderer
        get() = rendererFactory.MAP[this]
            ?: error("Renderer is not found for factory $this inside ${rendererFactory.MAP.name} renderer map")

    fun getEffectiveSeverity(languageVersionSettings: LanguageVersionSettings): Severity? {
        return when (languageVersionSettings.getFlag(AnalysisFlags.warningLevels)[name]) {
            WarningLevel.Error -> Severity.ERROR
            WarningLevel.Warning -> Severity.FIXED_WARNING
            WarningLevel.Disabled -> null
            null -> severity
        }
    }

    override fun toString(): String {
        return name
    }
}

class CjSourcelessDiagnosticFactory(
    name: String,
    severity: Severity,
    rendererFactory: BaseDiagnosticRendererFactory,
) : AbstractCjDiagnosticFactory(name, severity, rendererFactory) {
    fun create(message: String, location: CompilerMessageSourceLocation?, context: DiagnosticBaseContext): CjDiagnosticWithoutSource? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return CjDiagnosticWithoutSource(message, location, effectiveSeverity, this, context)
    }
}

sealed class CjDiagnosticFactoryN(
    name: String,
    severity: Severity,
    val defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    val psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory
) : AbstractCjDiagnosticFactory(name, severity, rendererFactory)

class CjDiagnosticFactory0(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
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

class CjDiagnosticFactory1<A>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
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

class CjDiagnosticFactory2<A, B>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
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

class CjDiagnosticFactory3<A, B, C>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
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

class CjDiagnosticFactory4<A, B, C, D>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : CjDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
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

sealed class CjDiagnosticFactoryForDeprecation<F : CjDiagnosticFactoryN>(
    val name: String,
    val deprecatingFeature: LanguageFeature,
    val warningFactory: F,
    val errorFactory: F
)

private const val WARNING = "_WARNING"
private const val ERROR = "_ERROR"

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



