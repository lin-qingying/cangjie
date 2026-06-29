package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.render

/**
 * class-like 类型短名渲染协议。
 *
 * 负责把类、接口、枚举、type alias 等类型上的短名按统一规则写入 [PrettyPrinter]，
 * 对齐 Kotlin Analysis API 中类似的 type name renderer。
 */
fun interface CaTypeNameRenderer {
    /**
     * 把 [name] 渲染到 [printer]。
     *
     * @param analysisSession 当前分析会话，提供 lifetime 校验。
     * @param name 待渲染的短名。
     * @param owner 名字所在类型，便于按类型决定是否加引号等。
     * @param typeRenderer 父级类型渲染器，用于复用子渲染器。
     * @param printer 输出目标。
     */
    fun renderName(
        analysisSession: CaSession,
        name: Name,
        owner: CaType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    /**
     * 始终调用 [Name.render]，按仓颉源码风格输出（必要时自动加引号转义）。
     */
    object QUOTED : CaTypeNameRenderer {
        /**
         * 按仓颉源码转义规则渲染类型短名。
         */
        override fun renderName(
            analysisSession: CaSession,
            name: Name,
            owner: CaType,
            typeRenderer: CaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append(name.render())
        }
    }
}
