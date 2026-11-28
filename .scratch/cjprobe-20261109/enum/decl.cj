// 枚举项与同名类型共存：值位置必须解析成枚举项。
// 官方 `Sema/EnumSugarTargetsFinder.cpp` 的 `RefineTargets` 在 ref 有 target type 且不是
// call/pattern 时，只保留 owner enum 与 target type 一致的枚举项候选，同名 class/struct 被淘汰。
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
