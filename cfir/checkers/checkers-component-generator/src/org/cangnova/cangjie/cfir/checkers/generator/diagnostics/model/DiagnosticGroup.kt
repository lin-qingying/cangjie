package org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.util.PrivateForInline
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.typeOf

/**
 * 诊断分组 DSL 基类。
 */
abstract class AbstractDiagnosticGroup @PrivateForInline constructor(val name: String, internal val containingObjectName: String) {
    @Suppress("PropertyName")
    @PrivateForInline
    /**
     * 当前分组内已注册的诊断定义。
     */
    val _diagnostics = mutableListOf<DiagnosticData>()

    @OptIn(PrivateForInline::class)
    /**
     * 当前分组内诊断定义的只读视图。
     */
    val diagnostics: List<DiagnosticData>
        get() = _diagnostics

    @OptIn(PrivateForInline::class)
    /**
     * 注册错误级别诊断。
     */
    internal inline fun <reified P : PsiElement> error(
        positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT,
        crossinline init: DiagnosticBuilder.Regular.() -> Unit = {}
    ) = diagnosticDelegateProvider<P>(Severity.ERROR, positioningStrategy, init)


    @OptIn(PrivateForInline::class)
    /**
     * 注册警告级别诊断。
     */
    internal inline fun <reified P : PsiElement> warning(
        positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT,
        crossinline init: DiagnosticBuilder.Regular.() -> Unit = {}
    ) = diagnosticDelegateProvider<P>(Severity.WARNING, positioningStrategy, init)

    @OptIn(PrivateForInline::class)
    /**
     * 注册由语言特性控制错误阶段的弃用诊断。
     */
    internal inline fun <reified P : PsiElement> deprecationError(
        featureForError: LanguageFeature,
        positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT,
        crossinline init: DiagnosticBuilder.Deprecation.() -> Unit = {}
    ) = deprecationDiagnosticDelegateProvider<P>(featureForError, positioningStrategy, init)

    @PrivateForInline
    /**
     * 创建普通诊断属性委托，并在委托绑定时记录诊断元数据。
     */
    internal inline fun <reified P : PsiElement> diagnosticDelegateProvider(
        severity: Severity,
        positioningStrategy: PositioningStrategy,
        crossinline init: DiagnosticBuilder.Regular.() -> Unit = {}
    ) = PropertyDelegateProvider<Any?, ReadOnlyProperty<AbstractDiagnosticGroup, RegularDiagnosticData>> { _, property ->
        val diagnostic = DiagnosticBuilder.Regular(
            containingObjectName,
            severity,
            name = property.name,
            psiType = typeOf<P>(),
            positioningStrategy,
        ).apply(init).build()
        _diagnostics += diagnostic
        ReadOnlyProperty { _, _ -> diagnostic }
    }

    @PrivateForInline
    /**
     * 创建弃用诊断属性委托，并在委托绑定时记录诊断元数据。
     */
    internal inline fun <reified P : PsiElement> deprecationDiagnosticDelegateProvider(
        featureForError: LanguageFeature,
        positioningStrategy: PositioningStrategy,
        crossinline init: DiagnosticBuilder.Deprecation.() -> Unit = {}
    ) = PropertyDelegateProvider<Any?, ReadOnlyProperty<AbstractDiagnosticGroup, DeprecationDiagnosticData>> { _, property ->
        val diagnostic = DiagnosticBuilder.Deprecation(
            containingObjectName,
            featureForError,
            name = property.name,
            psiType = typeOf<P>(),
            positioningStrategy,
        ).apply(init).build()
        _diagnostics += diagnostic
        ReadOnlyProperty { _, _ -> diagnostic }
    }

    @OptIn(PrivateForInline::class)
    /**
     * 合并同名诊断分组。
     */
    operator fun plus(other: AbstractDiagnosticGroup): AbstractDiagnosticGroup {
        require(name == other.name)

        val combinedDiagnostics = this.diagnostics + other.diagnostics

        return object : AbstractDiagnosticGroup(name, "#Stub") {
            init {
                _diagnostics.addAll(combinedDiagnostics)
            }
        }
    }
}

