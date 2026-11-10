// FILE: enumEntryShadowedDecl.cj
// 枚举项与可见的同名类型共存：裸引用解析成类型，不会回退到枚举项。
// 官方 `Sema/TypeCheckExpr/NameReferenceExpr.cpp` 的 `InferRefExpr` 先做普通 `Lookup`，只有结果为空
// （或全是宏函数）时才进入 `EnumSugarChecker`；`EnumSugarTargetsFinder::RefineTargets` 只在枚举项候选之间
// 按 target type 筛选，不参与与同名类型的竞争。同名 class/struct 命中后按类型引用处理，报
// `sema_ref_not_be_type` 与 `sema_mismatched_types`。cjc 1.0.0 / 1.0.5 / 1.1.3 实测一致：三处各报这两条。
enum ResponseCode {
    | Ok
    | AuthMoreData
    | Error
}

// 与枚举项 `AuthMoreData` 同名的包内类型。
protected class AuthMoreData {
}

// 与标准库类型同名（`std.core.String` 经默认导入可见）。
enum ColumnType {
    | Tiny
    | String
}

// FILE: enumEntryShadowedUse.cj
func ofCode(value: UInt8): ResponseCode {
    match (value) {
        case 0x00 => Ok
        case 0x01 => <!REF_NOT_BE_TYPE, TYPE_MISMATCH!>AuthMoreData<!>
        case 0xFF => <!REF_NOT_BE_TYPE, TYPE_MISMATCH!>Error<!>
        case _ => Ok
    }
}

func ofColumn(value: UInt8): ColumnType {
    match (value) {
        case 0x01 => Tiny
        case 0xFE => <!REF_NOT_BE_TYPE, TYPE_MISMATCH!>String<!>
        case _ => Tiny
    }
}
