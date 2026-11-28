package org.cangnova.cangjie.cfir.types

/**
 * Native InvalidTy 的非语义状态。它没有解析错误诊断，也不是 Unit/FuncTy；
 * 原零 type 槽、显式 Invalid 槽和 UNREACHABLE 签名省略的来源仍由各自 metadata 保存。
 */
data object ConeInvalidType : ConeRigidType(), ConeTypeConstructorMarker {
    override val attributes: ConeAttributes get() = ConeAttributes.Empty
}

