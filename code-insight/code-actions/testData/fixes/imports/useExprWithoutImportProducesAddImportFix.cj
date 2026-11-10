// USE_EXPR_WITHOUT_IMPORT：quote 表达式要求先导入 std.ast，诊断携带完整 importPath。
package quickfix.imports

func sample() {
    quote(1 + 1)
}
