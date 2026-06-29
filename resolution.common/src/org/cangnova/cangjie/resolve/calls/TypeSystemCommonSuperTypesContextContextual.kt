package org.cangnova.cangjie.resolve.calls

import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.RigidTypeMarker
import org.cangnova.cangjie.type.model.TypeSystemCommonSuperTypesContext

/**
 * 在 common-super-type 上下文中计算仓颉类型深度。
 */
context(c: TypeSystemCommonSuperTypesContext)
fun CangJieTypeMarker.typeDepth(): Int = with(c) { typeDepth() }
/**
 * 在 common-super-type 上下文中计算刚性类型深度。
 */
context(c: TypeSystemCommonSuperTypesContext)
fun RigidTypeMarker.typeDepth(): Int = with(c) { typeDepth() }
