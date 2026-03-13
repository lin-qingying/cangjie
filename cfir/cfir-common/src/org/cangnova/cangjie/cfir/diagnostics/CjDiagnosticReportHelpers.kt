@file:OptIn(InternalDiagnosticFactoryMethod::class)

package org.cangjie.cfir.diagnostics

import org.cangjie.cfir.source.AbstractCjSourceElement
import org.cangjie.cli.common.messages.CompilerMessageSourceLocation

// #### KtSourcelessFactory ####

context(context: DiagnosticContext)
fun DiagnosticReporter.report(
    factory: CjSourcelessDiagnosticFactory,
    message: String,
    location: CompilerMessageSourceLocation? = null,
) {
    report(factory.create(message, location, context), context)
}

// #### CjDiagnosticFactory0 ####

fun DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory0,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), positioningStrategy, context), context)
}

context(context: DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory0,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), positioningStrategy, context), context)
}

// #### CjDiagnosticFactory1 ####

fun <A> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, positioningStrategy, context), context)
}

context(context: DiagnosticContext)
fun <A> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory1<A>,
    a: A,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, positioningStrategy, context), context)
}

// #### CjDiagnosticFactory2 ####

fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, positioningStrategy, context), context)
}

context(context: DiagnosticContext)
fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, positioningStrategy, context), context)
}

// #### CjDiagnosticFactory3 ####

fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, c, positioningStrategy, context), context)
}

context(context: DiagnosticContext)
fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, c, positioningStrategy, context), context)
}

// #### CjDiagnosticFactory4 ####

fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, c, d, positioningStrategy, context), context)
}

context(context: DiagnosticContext)
fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, c, d, positioningStrategy, context), context)
}

fun AbstractCjSourceElement?.requireNotNull(): AbstractCjSourceElement =
    requireNotNull(this) { "source must not be null" }

// #### CjDiagnosticFactoryForDeprecation0 ####

fun DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation0,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), context, positioningStrategy)
}

context(context: DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation0,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), positioningStrategy)
}

// #### CjDiagnosticFactoryForDeprecation1 ####

fun <A> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, context, positioningStrategy)
}

context(context: DiagnosticContext)
fun <A> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation1<A>,
    a: A,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, positioningStrategy)
}

// #### CjDiagnosticFactoryForDeprecation2 ####

fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, context, positioningStrategy)
}

context(context: DiagnosticContext)
fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, positioningStrategy)
}

// #### CjDiagnosticFactoryForDeprecation3 ####

fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, c, context, positioningStrategy)
}

context(context: DiagnosticContext)
fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation3<A, B, C>,
    a: A,
    b: B,
    c: C,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, c, positioningStrategy)
}

// #### CjDiagnosticFactoryForDeprecation4 ####

fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, c, d, context, positioningStrategy)
}

context(context: DiagnosticContext)
fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, c, d, positioningStrategy)
}

fun <F : CjDiagnosticFactoryN> CjDiagnosticFactoryForDeprecation<F>.chooseFactory(context: DiagnosticContext): F {
    return if (context.languageVersionSettings.supportsFeature(deprecatingFeature)) {
        errorFactory
    } else {
        warningFactory
    }
}



