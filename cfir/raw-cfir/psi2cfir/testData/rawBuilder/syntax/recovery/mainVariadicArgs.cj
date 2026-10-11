// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：main 的形参写可变参数 `Array<String>...`（官方 main 形参只能是 `Array<String>`；`...` 只用于 C foreign 形参）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_character "expected ';' or '<NL>', found '...'"。
main(args: Array<String>...): Int64 {
    return 0
}
