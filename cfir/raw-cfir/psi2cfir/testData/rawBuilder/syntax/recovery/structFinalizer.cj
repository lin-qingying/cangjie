// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：struct 里声明 ~init（析构函数只支持 class）。
// 官方 cjc 1.0.5 / 1.1.3：parse_unexpected_declaration_in_scope "unexpected finalizer in struct body"。
struct WithFinalizer {
    var v: Int64 = 0

    ~init() {
    }
}
