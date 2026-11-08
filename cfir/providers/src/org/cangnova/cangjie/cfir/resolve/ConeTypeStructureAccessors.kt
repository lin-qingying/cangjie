package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.arrayLiteralElementType as coneArrayLiteralElementType

/**
 * `arrayLiteralElementType` 的 use-site 版本：先按 session 展开别名，再复用无 session 实现。
 *
 * 用于“库签名流入”且尚未展开的期望类型（跨包别名在反序列化期可能没有内联 `expandedType`）。
 * 已经展开过的调用点应直接用属性访问，避免重复一次 `fullyExpandedType`（内含 provider 查找）。
 */
fun ConeCangJieType.expandedArrayLiteralElementType(session: CfirSession): ConeCangJieType? =
    fullyExpandedType(session).coneArrayLiteralElementType
