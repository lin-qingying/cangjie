// 标识符：普通标识符、下划线与转义标识符（反引号）。
// 官方依据：Cangjie 文档《标识符》；cjc 1.0.5 / 1.1.3 零诊断。
func identifiers(): Unit {
    let plain = 1
    let camelCaseName = 2
    let snake_case_name = 3
    let name123 = 4
    let _ = 5
    let _leadingUnderscore = 6
    let trailingUnderscore_ = 7
    let 中文标识符 = 11
    let _ = (plain, camelCaseName, snake_case_name, name123)
    let _ = (_leadingUnderscore, trailingUnderscore_)
    let _ = 中文标识符
}
