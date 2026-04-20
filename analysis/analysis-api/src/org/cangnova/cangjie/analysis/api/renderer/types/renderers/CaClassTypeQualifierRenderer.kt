package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaClassTypeQualifier
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.ClassId

/**
 * class-like 类型限定名前缀渲染协议。
 *
 * 仓颉当前公开 `ClassId` 只表示顶层 class-like 声明，
 * 因此这里的 qualifier 只负责包路径前缀，不负责最终短名输出。
 */
fun interface CaClassTypeQualifierRenderer {
    fun renderClassTypeQualifier(
        analysisSession: CaSession,
        type: CaType,
        qualifiers: List<CaClassTypeQualifier>,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val WITH_QUALIFIED_NAMES: CaClassTypeQualifierRenderer =
            CaClassTypeQualifierRenderer { _, type, _, _, printer ->
                val packageFqName = (type as? CaClassLikeType)?.classId?.packageFqName ?: return@CaClassTypeQualifierRenderer
                if (!packageFqName.isRoot) {
                    printer {
                        append(packageFqName.asString())
                        append(".")
                    }
                }
            }

        val WITH_SHORT_NAMES: CaClassTypeQualifierRenderer = CaClassTypeQualifierRenderer { _, _, _, _, _ -> }
    }
}

/**
 * 声明 / 类型共享的 classId 包前缀渲染。
 */
fun CaClassTypeQualifierRenderer.renderClassIdQualifier(classId: ClassId?, printer: PrettyPrinter) {
    if (this !== CaClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES) return
    val packageFqName = classId?.packageFqName ?: return
    if (!packageFqName.isRoot) {
        printer {
            append(packageFqName.asString())
            append(".")
        }
    }
}
