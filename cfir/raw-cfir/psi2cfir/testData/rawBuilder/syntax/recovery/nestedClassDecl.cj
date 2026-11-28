// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：类体内嵌套类声明（仓颉的类型只能在顶层作用域声明）。
// 官方 cjc 1.0.5 / 1.1.3：parse_unexpected_declaration_in_scope "unexpected class declaration in class body"。
class Outer {
    class Inner {
    }
}
