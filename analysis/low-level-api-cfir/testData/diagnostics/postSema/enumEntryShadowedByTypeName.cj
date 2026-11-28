// MODULE: main
// MAIN_FILE_NAME: enumEntryShadowedByTypeName.cj
// FILE: enumEntryShadowedDecl.cj
package test

// 枚举项与可见的同名类型共存：裸引用解析成类型，不会回退到枚举项（官方
// `Sema/TypeCheckExpr/NameReferenceExpr.cpp` 的 `InferRefExpr` 只在普通 `Lookup` 为空时才走枚举语法糖；
// cjc 1.0.0 / 1.0.5 / 1.1.3 实测三处各报 `sema_ref_not_be_type` + `sema_mismatched_types`）。
enum ResponseCode {
    | Ok
    | AuthMoreData
    | Error
}

protected class AuthMoreData {
}

// 与标准库类型同名（`std.core.String` 经默认导入可见）。
enum ColumnType {
    | Tiny
    | String
}

// FILE: enumEntryShadowedByTypeName.cj
package test

func ofCode(value: UInt8): ResponseCode {
    match (value) {
        case 0x00 => Ok
        case 0x01 => AuthMoreData
        case 0xFF => Error
        case _ => Ok
    }
}

func ofColumn(value: UInt8): ColumnType {
    match (value) {
        case 0x01 => Tiny
        case 0xFE => String
        case _ => Tiny
    }
}
