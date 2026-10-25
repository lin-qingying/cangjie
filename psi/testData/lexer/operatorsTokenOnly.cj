// 仅词法形态：无独立合法宿主的 token 组合（<-、~>、|>、...、::、@!、$ 前缀、宏转义）。
// 说明：本文件专测词法器对这些 token 的组合输出，整体不是可编译程序
//（cjc 会拒绝；psi 词法测试以 golden 比对为准，属有意保留的例外）。
// 官方依据：仓颉词法 token 清单（CjTokens / CjLexer.flex）。
func tokenOnlyForms(): Unit {
    let bitwiseNot = ~a
    let composition = a ~> b
    let pipeline = a |> b
    let ellipsis = f(a...)
    let doubleColon = Type::member
    let atExcl = @!macro
    let dollar = $field
    let macroEscapes = @M(\(x\), \[y\], \@z)
    let _ = (bitwiseNot, composition, pipeline, ellipsis, doubleColon)
    let _ = (atExcl, dollar, macroEscapes)
}
