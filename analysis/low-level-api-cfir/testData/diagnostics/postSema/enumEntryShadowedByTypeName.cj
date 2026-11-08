// MODULE: main
// MAIN_FILE_NAME: enumEntryShadowedByTypeName.cj
// FILE: enumEntryShadowedDecl.cj
package test

// 枚举项与同名类型共存：值位置必须解析成枚举项（官方 `Sema/EnumSugarTargetsFinder.cpp`
// 的 `RefineTargets` 按 target type 保留 owner enum 一致的枚举项，同名 class 被淘汰）。
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
