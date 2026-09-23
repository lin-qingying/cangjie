// flow 表达式解糖调用失败的诊断归属（官方 `ChkFlowExpr` 第二阶段）。
//
// 官方 `external/cangjie_compiler/src/Sema/TypeCheckExpr/BinaryExpr.cpp:1057-1070`：
// 解糖后的调用检查失败时丢弃调用自身的**全部诊断**，用 `RecoverToBinaryExpr` 恢复成二元
// 节点，再经 `DiagnoseForBinaryExpr` 只在操作符上报 `sema_invalid_binary_expr`
//（本仓 `INVALID_BINARY_OPERATOR`，锚操作符、渲染两侧操作数类型分列两个实参）。
//
// 逐例经 cjc 1.0.5 与 1.0.0 实测一致（`codex-probes/flow_operand_failure/out105` 与 `out100`）。
// 操作数自身的名字解析失败**不走**该路径：官方在综合操作数阶段就已报出
// `sema_undeclared_identifier`（本仓 `UNRESOLVED_REFERENCE`），不再派生操作符诊断。
//
// 操作数归属类型的裁决对位官方 `DiagnoseForBinaryExpr`（`BinaryExpr.cpp:795-826`）：解糖调用
// 失败后操作数上残留的类型可能来自实参映射的**写回**而非其自身综合（`|>` 的实参节点与左操作
// 数是同一对象，见 `buildPipelineCall`），故在失败分支重新取一次操作数自身的综合结果；只有
// 两侧自身都成立时才在操作符上报诊断（`:838-843`）。`this |> g`（左操作数自身正确、调用实参
// 失配）由此收敛，不再需要阶段 1 式的投机综合基建。
func g(v: Int64): Int64 { v }

func takesC(v: FlowOperandFailure): Int64 { 1 }

class FlowOperandFailure {
    var x = 1

    // cjc: invalid binary operator '~>' on type 'Int64' and '(Int64) -> Int64'
    func valueCompose(): Unit {
        x <!INVALID_BINARY_OPERATOR!>~><!> g
    }

    // cjc 1.0.5 与 1.0.0 实测一致：`invalid binary operator '|>' on type 'This' and
    // '(Int64) -> Int64'`，锚 `|>` 两个字符（列 14-16）。证据：`codex-probes/
    // flow_operand_failure/p25_fixture_this_pipe_mismatch.cj` 的 out105/out100。
    func thisLeftPipe(): Unit {
        this <!INVALID_BINARY_OPERATOR!>|><!> g
    }

    // 正控：`this |> f` 在形参类型匹配时合法（cjc 零诊断）。
    func thisLeftPipeLegal(): Unit {
        this |> takesC
    }

    // cjc: invalid binary operator '|>' on type 'Int64' and 'Int64'
    func valuePipeFunctionPart(): Unit {
        1 <!INVALID_BINARY_OPERATOR!>|><!> x
    }

    // cjc: invalid binary operator '~>' on type 'Int64' and 'Int64'
    func valueComposeBoth(): Unit {
        x <!INVALID_BINARY_OPERATOR!>~><!> x
    }

    // 正控：函数值组合合法，零诊断。
    func controlCompose(): Unit {
        g ~> g
    }

    // 正控：管道合法，零诊断。
    func controlPipe(): Unit {
        1 |> g
    }
}

// 操作数级名字解析失败保留自身诊断，不派生操作符诊断（官方 `sema_undeclared_identifier`）。
func operandNameFailure(): Unit {
    <!UNRESOLVED_REFERENCE!>missing<!> ~> g
}

main() {}
