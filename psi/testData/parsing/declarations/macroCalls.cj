// 宏调用：宏声明、位置 / 具名实参、嵌套调用、内联属性实参与声明前属性宏。
// 官方依据：Cangjie 文档《宏》；cjc 1.0.5 / 1.1.3 零诊断。
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
