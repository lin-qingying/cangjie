package org.cangnova.cangjie.resolve.calls

import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.RigidTypeMarker
import org.cangnova.cangjie.type.model.TypeSystemCommonSuperTypesContext

context(c: TypeSystemCommonSuperTypesContext)
fun CangJieTypeMarker.typeDepth(): Int = with(c) { typeDepth() }
context(c: TypeSystemCommonSuperTypesContext)
fun RigidTypeMarker.typeDepth(): Int = with(c) { typeDepth() }
