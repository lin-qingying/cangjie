// RUN_PIPELINE_TILL: FRONTEND
// 宏包与宏声明：macro package 声明、public 宏（宏声明必须 public）、形参必须是 Tokens（不能具名 / 非 Tokens 类型，见 recovery）、
// 宏体三种写法：透传 Tokens、quote(hello) 直接写 token、quote({ $(input) }) 生成代码块（quote 内的代码块插值语法是 `$(x)`）。
// 官方依据：Cangjie 文档《宏定义》《Tokens 相关类型》；cjc 1.0.5 / 1.1.3 零诊断。
macro package macrodecls
import std.ast.*

public macro identity(input: Tokens): Tokens {
    return input
}

public macro greeting(input: Tokens): Tokens {
    return quote(hello)
}

public macro withBlock(input: Tokens): Tokens {
    return quote(
        {
            $(input)
        }
    )
}
