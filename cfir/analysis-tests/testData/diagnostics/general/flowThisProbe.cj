// flow 表达式操作数的裸 this/super 行为。
// 官方依据：`external/cangjie_compiler/src/Sema/TypeCheckExpr/BinaryExpr.cpp:1008-1075`（ChkFlowExpr）
//   `(op == COMPOSITION && !isThisExpr(left)) || !isThisExpr(right)`
//   即 `~>` 两侧、`|>` 的函数部分都不允许裸 `this`；`this |> f` 合法。
// 裸 `super` 不由该判据负责（官方 `InferSuperExpr` 报 sema_illegal_super_alone），
// flow 失败本身另报二元运算符诊断。
open class Base {}

class FlowProbe <: Base {
    var x = 1

    func f(v: Int64): Int64 { v }

    func thisPipe(): Unit {
        1 |> <!FLOW_EXPRESSIONS_USE_THIS_OR_SUPER!>this<!>
    }

    func fieldCompose(): Unit {
        x ~> <!FLOW_EXPRESSIONS_USE_THIS_OR_SUPER!>this<!>
    }

    func localCompose(): Unit {
        let y = 1
        y ~> <!FLOW_EXPRESSIONS_USE_THIS_OR_SUPER!>this<!>
    }

    func superPipe(): Unit {
        1 <!INVALID_BINARY_OPERATOR!>|><!> <!ILLEGAL_SUPER_ALONE!>super<!>
    }

    func superCompose(): Unit {
        x <!INVALID_BINARY_OPERATOR!>~><!> <!ILLEGAL_SUPER_ALONE!>super<!>
    }

    func thisLeftCompose(): Unit {
        <!FLOW_EXPRESSIONS_USE_THIS_OR_SUPER!>this<!> ~> f
    }

    func controlPipe(): Unit {
        1 |> f
    }
}

main() {
    1 |> <!FLOW_EXPRESSIONS_USE_THIS_OR_SUPER!>this<!>
}
