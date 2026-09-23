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
// 已知缺口 1 项（`this |> g`，见下方注释）：解糖调用的**实参检查**把失配的错误类型写回左
// 操作数（实参节点与操作数节点是同一对象），操作数的根错误检查据此提前返回，操作符诊断
// 无从产生。官方用投机综合（`DiagSuppressor` + `PData::Reset`，`BinaryExpr.cpp:1021-1027`）
// 避免该污染；本仓 transform 对 lambda 形参左操作数不具可重入性（预综合抛内部异常），
// 故需要与官方对位的投机综合/回滚基建后才能闭合。
func g(v: Int64): Int64 { v }

func takesC(v: FlowOperandFailure): Int64 { 1 }

class FlowOperandFailure {
    var x = 1

    // cjc: invalid binary operator '~>' on type 'Int64' and '(Int64) -> Int64'
    func valueCompose(): Unit {
        x <!INVALID_BINARY_OPERATOR!>~><!> g
    }

    // 已知缺口：cjc 报 `invalid binary operator '|>' on type 'This' and '(Int64) -> Int64'`，
    // CFIR 当前不报（也不会再报出官方不存在的 ARGUMENT_TYPE_MISMATCH）。
    func thisLeftPipe(): Unit {
        this |> g
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
