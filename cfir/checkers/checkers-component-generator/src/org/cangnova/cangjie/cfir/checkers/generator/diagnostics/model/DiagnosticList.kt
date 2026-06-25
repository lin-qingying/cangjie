package org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.util.PrivateForInline
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * 一组诊断定义的生成 DSL 根对象。
 */
abstract class DiagnosticList(internal val objectName: String) {

    /**
     * 当前诊断列表定义类的全限定名，用于生成文件溯源。
     */
    val diagnosticListDefinitionFQN = this::class.qualifiedName

    @Suppress("PropertyName")
    @PrivateForInline
    /**
     * DSL 注册到当前诊断列表的分组集合。
     */
    val _groups = mutableListOf<AbstractDiagnosticGroup>()

    @OptIn(PrivateForInline::class)
    /**
     * 已注册诊断分组的只读视图。
     */
    val groups: List<AbstractDiagnosticGroup>
        get() = _groups

    @OptIn(PrivateForInline::class)
    /**
     * 所有分组中诊断定义的展平列表。
     */
    val allDiagnostics: List<DiagnosticData>
        get() = groups.flatMap { it.diagnostics }


    @OptIn(PrivateForInline::class)
    /**
     * 将诊断分组属性委托注册到当前诊断列表。
     */
    operator fun DiagnosticGroup.provideDelegate(
        thisRef: DiagnosticList,
        prop: KProperty<*>
    ): ReadOnlyProperty<DiagnosticList, DiagnosticGroup> {
        val group = this
        _groups += group
        return ReadOnlyProperty { _, _ -> group }
    }

    @OptIn(PrivateForInline::class)
    /**
     * 合并两个诊断列表，并按分组名称合并同名分组。
     */
    operator fun plus(other: DiagnosticList): DiagnosticList {
        val groupsByName = mutableMapOf<String, MutableList<AbstractDiagnosticGroup>>()

        fun collect(groups: List<AbstractDiagnosticGroup>) {
            for (group in groups) {
                val list = groupsByName.getOrPut(group.name) { mutableListOf() }
                list += group
            }
        }

        collect(groups)
        collect(other.groups)

        val resultingGroups = groupsByName.values.map {
            it.reduce { acc, group -> acc + group }
        }

        return object : DiagnosticList("#Stub") {
            init {
                _groups.addAll(resultingGroups)
            }
        }
    }

    @PrivateForInline
    /**
     * 隶属于当前诊断列表的诊断分组 DSL。
     */
    abstract inner class DiagnosticGroup(name: String) : AbstractDiagnosticGroup(name, objectName)
}

/**
 * 单个诊断定义的构造器基类。
 */
sealed class DiagnosticBuilder(
    /**
     * 诊断所属顶层对象名。
     */
    protected val containingObjectName: String,
    /**
     * 诊断工厂名称。
     */
    protected val name: String,
    /**
     * 诊断锚定的 PSI 类型。
     */
    protected val psiType: KType,
    /**
     * 诊断源码定位策略。
     */
    protected val positioningStrategy: PositioningStrategy,
) {
    /**
     * 普通诊断构造器。
     */
    class Regular(
        containingObjectName: String,
        /**
         * 普通诊断的严重级别。
         */
        private val severity: Severity,
        name: String,
        psiType: KType,
        positioningStrategy: PositioningStrategy,
    ) : DiagnosticBuilder(containingObjectName, name, psiType, positioningStrategy) {
        /**
         * 当前普通诊断是否允许 suppress。
         */
        var isSuppressible: Boolean = false

        @OptIn(PrivateForInline::class)
        /**
         * 构建普通诊断元数据。
         */
        override fun build(): RegularDiagnosticData {
            return RegularDiagnosticData(
                containingObjectName,
                severity,
                name,
                psiType,
                parameters,
                positioningStrategy,
                isSuppressible,
            )
        }
    }

    /**
     * 弃用诊断构造器。
     */
    class Deprecation(
        containingObjectName: String,
        /**
         * 控制该弃用诊断进入错误阶段的语言特性。
         */
        private val featureForError: LanguageFeature,
        name: String,
        psiType: KType,
        positioningStrategy: PositioningStrategy,
    ) : DiagnosticBuilder(containingObjectName, name, psiType, positioningStrategy) {
        @OptIn(PrivateForInline::class)
        /**
         * 构建弃用诊断元数据。
         */
        override fun build(): DeprecationDiagnosticData {
            return DeprecationDiagnosticData(
                containingObjectName,
                featureForError,
                name,
                psiType,
                parameters,
                positioningStrategy,
            )
        }
    }

    @PrivateForInline
    /**
     * 已注册的诊断参数。
     */
    val parameters = mutableListOf<DiagnosticParameter>()

    @OptIn(PrivateForInline::class)
    /**
     * 为当前诊断追加一个类型安全参数。
     */
    inline fun <reified T> parameter(name: String) {
        if (parameters.size >= MAX_DIAGNOSTIC_PARAMETER_COUNT) {
            error("Diagnostic cannot have more than $MAX_DIAGNOSTIC_PARAMETER_COUNT parameters")
        }
        parameters += DiagnosticParameter(
            name = name,
            type = typeOf<T>()
        )
    }

    /**
     * 构建最终诊断元数据对象。
     */
    abstract fun build(): DiagnosticData

    /**
     * 诊断构造器常量。
     */
    companion object {
        /**
         * 单个诊断允许携带的最大参数数量。
         */
        const val MAX_DIAGNOSTIC_PARAMETER_COUNT = 4
    }
}

/**
 * 构造生成诊断容器 KDoc 使用的溯源文本。
 */
fun DiagnosticList.extendedKDoc(defaultKDoc: String? = null): String = buildString {
    if (defaultKDoc != null) {
        appendLine(defaultKDoc)
        appendLine()
    }
    append("Generated from: [$diagnosticListDefinitionFQN]")
}

