package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 面向源码风格的类型形参 renderer 预设。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeParametersRendererForSource`。
 */
object CaTypeParametersRendererForSource {
    /**
     * 预设: 名字写在 `<...>` 内, 上界统一放到尾部 `where` 子句, 多上界以 ` & ` 分隔。
     *
     * 这贴近仓颉源码的常见排版, 避免在尖括号内堆积过多约束。
     */
    val WITH_BOUNDS_IN_WHERE_CLAUSE: CaTypeParametersRenderer = CaTypeParametersRenderer { analysisSession, owner, declarationRenderer, printer ->
        val renderedTypeParameters = owner.typeParameters.filter { typeParameter ->
            declarationRenderer.typeParametersFilter.shouldRenderTypeParameter(analysisSession, owner, typeParameter)
        }
        if (renderedTypeParameters.isEmpty()) return@CaTypeParametersRenderer

        // 类型形参没有显式上界时，符号层会补一个隐式 `Any` 上界；源码渲染不应把它展开成
        // `where T <: Any`。否则 renderer 会去解析 `std/core/Any`，在库/反编译声明上失败，
        // 导致整个声明（以及引用该声明的 Quick Doc）退化为空。
        val boundedParameters = renderedTypeParameters.mapNotNull { typeParameter ->
            val explicitBounds = typeParameter.upperBounds.filterNot { bound -> bound.isImplicitAnyUpperBound() }
            if (explicitBounds.isEmpty()) null else typeParameter to explicitBounds
        }
        printer {
            printCollection(
                renderedTypeParameters,
                prefix = "<",
                postfix = ">",
            ) { typeParameter ->
                declarationRenderer.codeStyle.getSeparatorBetweenAnnotationAndOwner(analysisSession, typeParameter).separated(
                    {
                        declarationRenderer.annotationRenderer.renderAnnotations(
                            analysisSession,
                            typeParameter,
                            this,
                        )
                    },
                    {
                        declarationRenderer.modifiersRenderer.renderDeclarationModifiers(
                            analysisSession,
                            typeParameter,
                            this,
                        )
                    },
                    {
                        declarationRenderer.nameRenderer.renderName(
                            analysisSession,
                            typeParameter,
                            declarationRenderer,
                            this,
                        )
                    },
                )
            }

            if (boundedParameters.isNotEmpty()) {
                append(" where ")
                printCollection(
                    boundedParameters,
                ) { (typeParameter, explicitBounds) ->
                    append(typeParameter.name.asString())
                    append(" <: ")
                    printCollection(
                        explicitBounds,
                        separator = " & ",
                    ) { upperBound ->
                        declarationRenderer.typeRenderer.renderType(
                            analysisSession,
                            declarationRenderer.declarationTypeApproximator.approximateType(
                                analysisSession,
                                upperBound,
                            ),
                            this,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 隐式 `Any` 上界：符号层为每个无显式约束的类型形参补的默认上界。
 */
private fun CaType.isImplicitAnyUpperBound(): Boolean =
    this is CaClassLikeType &&
        classId.packageFqName.asString() == "std.core" &&
        classId.shortClassName.asString() == "Any"
