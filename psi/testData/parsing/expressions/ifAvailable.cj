// @IfAvailable 表达式：按 API level 选择两个分支的形式。
// 官方依据：Cangjie 文档《条件编译》；cjc 1.0.5 / 1.1.3 零诊断。
func ifAvailableDemo(): Int64 {
    @IfAvailable(level: 12, { => 1 }, { => 2 })
    return 1
}

func ifAvailableChained(flag: Bool): Int64 {
    let v = @IfAvailable(level: 20, { => 10 }, { => 20 })
    let _ = flag
    return v
}
