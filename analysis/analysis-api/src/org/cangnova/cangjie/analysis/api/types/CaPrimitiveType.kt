package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/**
 * 仓颉基本类型 public model。
 *
 * 基本类型不是普通 class-like type，不暴露 classId / symbol / qualifiers。
 * 公开层直接对齐 CFIR 的 [PrimitiveTypeKind]，避免使用弱语义字符串。
 */
abstract class CaPrimitiveType : CaType {
    abstract val kind: PrimitiveTypeKind

    abstract override fun createPointer(): CaTypePointer<CaPrimitiveType>
}
