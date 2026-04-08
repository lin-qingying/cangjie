package org.cangnova.cangjie.analysis.api.renderer.types

import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.CaPrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.name.ClassId

/**
 * 类型渲染位置。
 *
 * 当前仓颉公开类型系统尚未完全公开 variance 投影模型，
 * 因此 renderer 位置枚举只承担“近似方向”的公共语义。
 */
enum class CaTypeRendererPosition {
    INVARIANT,
    IN_VARIANCE,
    OUT_VARIANCE,
}

/**
 * class-like 类型限定名渲染协议。
 *
 * 仓颉没有嵌套类语义，因此这里的 qualifier 只负责“包路径前缀”，
 * 真正的类型名由 [CaTypeNameRenderer] 单独负责。
 */
fun interface CaClassTypeQualifierRenderer {
    fun renderQualifier(classId: ClassId, printer: CaPrettyPrinter)

    companion object {
        val WITH_QUALIFIED_NAMES: CaClassTypeQualifierRenderer = CaClassTypeQualifierRenderer { classId, printer ->
            val packageFqName = classId.packageFqName
            if (!packageFqName.isRoot) {
                printer.append(packageFqName.asString())
                printer.append(".")
            }
        }

        val WITH_SHORT_NAMES: CaClassTypeQualifierRenderer = CaClassTypeQualifierRenderer { _, _ -> }
    }
}

/**
 * class-like 类型短名渲染协议。
 *
 * 该层与 qualifier 分开后，preset 可以分别控制：
 * 1. 是否输出包级限定前缀；
 * 2. 类型名本身如何书写。
 */
fun interface CaTypeNameRenderer {
    fun renderName(classId: ClassId, printer: CaPrettyPrinter)

    companion object {
        val QUOTED: CaTypeNameRenderer = CaTypeNameRenderer { classId, printer ->
            printer.append(classId.shortClassName.asString())
        }
    }
}

/**
 * 单个类型实参渲染协议。
 *
 * 目前仓颉公开类型系统还没有把 variance projection 暴露出来，
 * 因此这里先稳定承接“普通类型实参”的渲染。
 */
fun interface CaTypeArgumentRenderer {
    fun renderTypeArgument(argument: CaType, typeRenderer: CaTypeRenderer, printer: CaPrettyPrinter)

    companion object {
        val AS_SOURCE: CaTypeArgumentRenderer = CaTypeArgumentRenderer { argument, typeRenderer, printer ->
            printer.append(typeRenderer.renderType(argument))
        }
    }
}

/**
 * 类型实参列表渲染协议。
 */
fun interface CaTypeArgumentsRenderer {
    fun renderTypeArguments(type: CaClassLikeType, typeRenderer: CaTypeRenderer, printer: CaPrettyPrinter)

    companion object {
        val NO_TYPE_ARGUMENTS: CaTypeArgumentsRenderer = CaTypeArgumentsRenderer { _, _, _ -> }

        val AS_SOURCE: CaTypeArgumentsRenderer = CaTypeArgumentsRenderer { type, typeRenderer, printer ->
            if (type.typeArguments.isEmpty()) return@CaTypeArgumentsRenderer

            printer.append("<")
            type.typeArguments.forEachIndexed { index, argument ->
                if (index > 0) {
                    printer.append(", ")
                }
                typeRenderer.typeArgumentRenderer.renderTypeArgument(argument, typeRenderer, printer)
            }
            printer.append(">")
        }
    }
}

fun interface CaRendererTypeApproximator {
    fun approximateType(type: CaType, position: CaTypeRendererPosition): CaType

    companion object {
        val NO_APPROXIMATION: CaRendererTypeApproximator = CaRendererTypeApproximator { type, _ -> type }

        /**
         * 当前首版只暴露“可记名类型近似”的公共入口；
         * 在未补齐通用低层近似协议之前，保持恒等近似，避免伪造语义。
         */
        val TO_DENOTABLE: CaRendererTypeApproximator = NO_APPROXIMATION
    }
}

fun interface CaFunctionalTypeRenderer {
    fun renderFunctionalType(typeRenderer: CaTypeRenderer, type: CaFunctionType, printer: CaPrettyPrinter)

    companion object {
        val AS_SOURCE: CaFunctionalTypeRenderer = CaFunctionalTypeRendererForSource.WITH_KIND_KEYWORDS
    }
}

fun interface CaErrorTypeRenderer {
    fun renderErrorType(type: CaType, printer: CaPrettyPrinter)

    companion object {
        val AS_PRESENTATION: CaErrorTypeRenderer = CaErrorTypeRenderer { type, printer ->
            printer.append(type.presentation)
        }

        /**
         * 当前公开 `CaType` 还没有结构化错误诊断载体，
         * 因此 debug renderer 先与 `AS_PRESENTATION` 等价对齐 preset 名位。
         */
        val WITH_ERROR_MESSAGE: CaErrorTypeRenderer = AS_PRESENTATION
    }
}

/**
 * 仓颉公开类型 renderer。
 *
 * 这一层只依赖公开 `CaType` 结构，不依赖 CFIR 内部类型树。
 */
class CaTypeRenderer private constructor(
    val classIdRenderer: CaClassTypeQualifierRenderer,
    val typeNameRenderer: CaTypeNameRenderer,
    val typeApproximator: CaRendererTypeApproximator,
    val typeArgumentRenderer: CaTypeArgumentRenderer,
    val typeArgumentsRenderer: CaTypeArgumentsRenderer,
    val usualClassTypeRenderer: CaUsualClassTypeRenderer,
    val functionalTypeRenderer: CaFunctionalTypeRenderer,
    val tupleTypeRenderer: CaTupleTypeRenderer,
    val intersectionTypeRenderer: CaIntersectionTypeRenderer,
    val unionTypeRenderer: CaUnionTypeRenderer,
    val errorTypeRenderer: CaErrorTypeRenderer,
    val keywordsRenderer: CaKeywordsRenderer,
) {
    fun renderType(
        type: CaType,
        position: CaTypeRendererPosition = CaTypeRendererPosition.INVARIANT,
    ): String = prettyPrint {
        renderType(typeApproximator.approximateType(type, position), this)
    }

    /**
     * 渲染公开 `ClassId`。
     *
     * 该入口供 declaration renderer 与 type renderer 共享，
     * 保证“声明名里的 class-like 名称”和“类型里的 class-like 名称”使用同一套 name/qualifier 规则。
     */
    fun renderClassId(classId: ClassId): String = prettyPrint {
        renderClassId(classId, this)
    }

    internal fun renderClassId(classId: ClassId, printer: CaPrettyPrinter) {
        classIdRenderer.renderQualifier(classId, printer)
        typeNameRenderer.renderName(classId, printer)
    }

    private fun renderType(
        type: CaType,
        printer: CaPrettyPrinter,
    ) {
        when (type) {
            is CaClassLikeType -> usualClassTypeRenderer.renderType(type, this, printer)
            is CaFunctionType -> functionalTypeRenderer.renderFunctionalType(this, type, printer)
            is CaTupleType -> tupleTypeRenderer.renderType(type, this, printer)
            is CaIntersectionType -> intersectionTypeRenderer.renderType(type, this, printer)
            is CaUnionType -> unionTypeRenderer.renderType(type, this, printer)
            else -> errorTypeRenderer.renderErrorType(type, printer)
        }
    }

    fun with(action: Builder.() -> Unit): CaTypeRenderer {
        val current = this
        return Builder().apply {
            classIdRenderer = current.classIdRenderer
            typeNameRenderer = current.typeNameRenderer
            typeApproximator = current.typeApproximator
            typeArgumentRenderer = current.typeArgumentRenderer
            typeArgumentsRenderer = current.typeArgumentsRenderer
            usualClassTypeRenderer = current.usualClassTypeRenderer
            functionalTypeRenderer = current.functionalTypeRenderer
            tupleTypeRenderer = current.tupleTypeRenderer
            intersectionTypeRenderer = current.intersectionTypeRenderer
            unionTypeRenderer = current.unionTypeRenderer
            errorTypeRenderer = current.errorTypeRenderer
            keywordsRenderer = current.keywordsRenderer
            action()
        }.build()
    }

    class Builder {
        lateinit var classIdRenderer: CaClassTypeQualifierRenderer
        lateinit var typeNameRenderer: CaTypeNameRenderer
        lateinit var typeApproximator: CaRendererTypeApproximator
        lateinit var typeArgumentRenderer: CaTypeArgumentRenderer
        lateinit var typeArgumentsRenderer: CaTypeArgumentsRenderer
        lateinit var usualClassTypeRenderer: CaUsualClassTypeRenderer
        lateinit var functionalTypeRenderer: CaFunctionalTypeRenderer
        lateinit var tupleTypeRenderer: CaTupleTypeRenderer
        lateinit var intersectionTypeRenderer: CaIntersectionTypeRenderer
        lateinit var unionTypeRenderer: CaUnionTypeRenderer
        lateinit var errorTypeRenderer: CaErrorTypeRenderer
        lateinit var keywordsRenderer: CaKeywordsRenderer

        fun build(): CaTypeRenderer = CaTypeRenderer(
            classIdRenderer = classIdRenderer,
            typeNameRenderer = typeNameRenderer,
            typeApproximator = typeApproximator,
            typeArgumentRenderer = typeArgumentRenderer,
            typeArgumentsRenderer = typeArgumentsRenderer,
            usualClassTypeRenderer = usualClassTypeRenderer,
            functionalTypeRenderer = functionalTypeRenderer,
            tupleTypeRenderer = tupleTypeRenderer,
            intersectionTypeRenderer = intersectionTypeRenderer,
            unionTypeRenderer = unionTypeRenderer,
            errorTypeRenderer = errorTypeRenderer,
            keywordsRenderer = keywordsRenderer,
        )
    }

    companion object {
        operator fun invoke(action: Builder.() -> Unit): CaTypeRenderer =
            Builder().apply(action).build()
    }
}
