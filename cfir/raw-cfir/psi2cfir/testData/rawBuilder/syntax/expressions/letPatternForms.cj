// RUN_PIPELINE_TILL: FRONTEND
// let 模式（`<-` 写法）：if-let、while-let（配合 Option 的 getOrThrow）、直接给模式（`let Some(v) <- o`）。
// 注意：let 模式用 `<-` 而不是 `=`（官方 parse_expected_backarrow_in_let_cond）；for 里不支持 let 模式（官方 parse_expected_pattern）。
// 官方依据：官方 ParseExpr.cpp ParseLetPattern / combinedBackarrow（`<` 与 `-` 合并）；cjc 1.0.5 / 1.1.3 零诊断。
func letPatternForms(o: ?Int64): Int64 {
    var t = 0
    if (let v <- o) {
        t = v.getOrThrow()
    }
    while (let v <- o) {
        t += v.getOrThrow()
        break
    }
    if (let Some(v) <- o) {
        t += v
    }
    return t
}
