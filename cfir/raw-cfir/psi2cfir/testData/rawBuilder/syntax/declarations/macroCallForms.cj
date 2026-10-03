// RUN_PIPELINE_TILL: FRONTEND
// 宏调用形态：@M(...) 位置实参 / 具名实参、@M(...) 作为表达式 / 实参 / 嵌套调用、@M[...] 内联属性实参、@M 作为声明前的属性宏。
// 注意：官方不允许宏定义与宏调用在同一包（本文件同时给出两者以便 rawBuilder 覆盖语法），故 cjc 1.0.5 / 1.1.3 报
//   sema_unexpect_def_and_call_in_same_pkg "macro's call and definition can not in one package"——这是语义限制，语法部分 1.0.5 / 1.1.3 均通过。
// 官方依据：Cangjie 文档《宏的调用》；cjc 1.0.5 / 1.1.3 仅有上面这一条语义诊断。
macro package macrocalls
import std.ast.*

public macro echo(input: Tokens): Tokens {
    return input
}

public macro pair(first: Tokens, second: Tokens): Tokens {
    return quote($(first) + $(second))
}

func macroCalls(): Int64 {
    let a = @echo(1)
    let b = @echo(@echo(2))
    let c = @echo(@pair(3, 4))
    let d = @pair(@echo(5), 6)
    let e = @echo(1) + @echo(2)
    return a + b + c + d + e
}

@echo[1, 2]
func attributed(): Int64 {
    return 1
}
