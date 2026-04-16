package org.cangnova.cangjie.analysis.api.renderer.declarations

/**
 * renderer 代码风格协议。
 *
 * 当前先稳定控制最常用的空白策略，后续如果仓颉源码风格需要继续细化，
 * 可以在这一层继续扩展。
 */
interface CaRendererCodeStyle {
    val spaceAfterColon: Boolean

    val spaceAfterComma: Boolean
}
