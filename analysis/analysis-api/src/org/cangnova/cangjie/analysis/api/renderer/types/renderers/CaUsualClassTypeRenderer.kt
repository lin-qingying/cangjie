package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType

/**
 * [CaUsualClassType] 的渲染策略。
 *
 * 对齐 Kotlin `KaUsualClassTypeRenderer`，用于把普通的类/接口/枚举/记录类型连同它的注解、
 * 限定符与类型实参一起渲染到 [PrettyPrinter]。
 */
fun interface CaUsualClassTypeRenderer {

    /**
     * 把 [type] 渲染到 [printer]。
     *
     * @param analysisSession 当前分析会话，提供 lifetime 校验与符号查找。
     * @param type 待渲染的类类型。
     * @param typeRenderer 父级类型渲染器，用于复用注解、限定符、名字等子渲染器。
     * @param printer 输出目标。
     */
    fun renderType(
        analysisSession: CaSession,
        type: CaUsualClassType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 标准源码风格：注解 + 限定符 + 类名 + `<T1, T2, ...>` 形式的类型实参列表。
         */
        val AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS: CaUsualClassTypeRenderer =
            CaUsualClassTypeRenderer { analysisSession, type, typeRenderer, printer ->
                printer {
                    " ".separated(
                        { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, this) },
                        {
                            typeRenderer.classIdRenderer.renderClassTypeQualifier(analysisSession, type, type.qualifiers, typeRenderer, this)
                            typeRenderer.typeNameRenderer.renderName(
                                analysisSession,
                                type.classId.shortClassName,
                                type,
                                typeRenderer,
                                this,
                            )
                            printCollectionIfNotEmpty(
                                type.typeArguments,
                                prefix = "<",
                                postfix = ">",
                            ) { typeArgument ->
                                typeRenderer.renderType(analysisSession, typeArgument, this)
                            }
                        },
                    )
                }
            }

        /**
         * 与 [AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS] 相同，但省略类型实参列表，适合需要简短展示
         * 类引用本身的场景（如错误信息、stack trace 风格输出）。
         */
        val AS_CLASS_TYPE_WITHOUT_TYPE_ARGUMENTS: CaUsualClassTypeRenderer =
            CaUsualClassTypeRenderer { analysisSession, type, typeRenderer, printer ->
                printer {
                    " ".separated(
                        { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, this) },
                        {
                            typeRenderer.classIdRenderer.renderClassTypeQualifier(analysisSession, type, type.qualifiers, typeRenderer, this)
                            typeRenderer.typeNameRenderer.renderName(
                                analysisSession,
                                type.classId.shortClassName,
                                type,
                                typeRenderer,
                                this,
                            )
                        },
                    )
                }
            }
    }
}
