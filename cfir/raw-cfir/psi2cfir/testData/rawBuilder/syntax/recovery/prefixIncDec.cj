// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：前缀自增自减（官方只有后缀形式）。
// 官方 cjc 1.0.5 / 1.1.3：parse 阶段错误（expected expression / operator）。
func prefixIncDec(): Int64 {
    var a = 1
    ++a
    return a
}
