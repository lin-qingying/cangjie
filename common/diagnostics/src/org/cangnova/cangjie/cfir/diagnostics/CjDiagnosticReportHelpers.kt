@file:OptIn(InternalDiagnosticFactoryMethod::class)

package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.source.AbstractCjSourceElement


// #### KtSourcelessFactory ####

/**
 * 在 context receiver 提供的诊断上下文中上报无源码诊断。
 */
context(context: DiagnosticContext)
fun DiagnosticReporter.report(
    factory: CjSourcelessDiagnosticFactory,
    message: String,
    location: CompilerMessageSourceLocation? = null,
) {
    report(factory.create(message, location, context), context)
}

// #### CjDiagnosticFactory0 ####

/**
 * 使用显式诊断上下文上报无参数源码诊断。
 */
fun DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory0,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), positioningStrategy, context), context)
}

/**
 * 使用 context receiver 提供的诊断上下文上报无参数源码诊断。
 */
context(context: DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory0,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), positioningStrategy, context), context)
}

// #### CjDiagnosticFactory1 ####

/**
 * 使用显式诊断上下文上报一参数源码诊断。
 */
fun <A> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactory1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, positioningStrategy, context), context)
}

/**
 * 使用 context receiver 提供的诊断上下文上报一参数源码诊断。
 */
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

/**
 * 使用显式诊断上下文上报二参数源码诊断。
 */
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

/**
 * 使用 context receiver 提供的诊断上下文上报二参数源码诊断。
 */
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

/**
 * 使用显式诊断上下文上报三参数源码诊断。
 */
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

/**
 * 使用 context receiver 提供的诊断上下文上报三参数源码诊断。
 */
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

/**
 * 使用显式诊断上下文上报四参数源码诊断。
 */
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

/**
 * 使用 context receiver 提供的诊断上下文上报四参数源码诊断。
 */
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

/**
 * 要求源码元素非空，并在缺失时报告诊断调用错误。
 */
fun AbstractCjSourceElement?.requireNotNull(): AbstractCjSourceElement =
    requireNotNull(this) { "source must not be null" }

// #### CjDiagnosticFactoryForDeprecation0 ####

/**
 * 使用显式诊断上下文上报无参数退化特性诊断。
 */
fun DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation0,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), context, positioningStrategy)
}

/**
 * 使用 context receiver 提供的诊断上下文上报无参数退化特性诊断。
 */
context(context: DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation0,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), positioningStrategy)
}

// #### CjDiagnosticFactoryForDeprecation1 ####

/**
 * 使用显式诊断上下文上报一参数退化特性诊断。
 */
fun <A> DiagnosticReporter.reportOn(
    source: AbstractCjSourceElement?,
    factory: CjDiagnosticFactoryForDeprecation1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, context, positioningStrategy)
}

/**
 * 使用 context receiver 提供的诊断上下文上报一参数退化特性诊断。
 */
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

/**
 * 使用显式诊断上下文上报二参数退化特性诊断。
 */
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

/**
 * 使用 context receiver 提供的诊断上下文上报二参数退化特性诊断。
 */
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

/**
 * 使用显式诊断上下文上报三参数退化特性诊断。
 */
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

/**
 * 使用 context receiver 提供的诊断上下文上报三参数退化特性诊断。
 */
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

/**
 * 使用显式诊断上下文上报四参数退化特性诊断。
 */
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

/**
 * 使用 context receiver 提供的诊断上下文上报四参数退化特性诊断。
 */
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

/**
 * 根据当前语言版本特性支持情况选择 warning 或 error 诊断工厂。
 */
fun <F : CjDiagnosticFactoryN> CjDiagnosticFactoryForDeprecation<F>.chooseFactory(context: DiagnosticContext): F {
    return if (context.languageVersionSettings.supportsFeature(deprecatingFeature)) {
        errorFactory
    } else {
        warningFactory
    }
}


