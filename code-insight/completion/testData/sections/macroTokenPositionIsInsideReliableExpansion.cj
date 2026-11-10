package completion.sections

macro Foo(attrTokens: Tokens, inputTokens: Tokens): Tokens {
    attrTokens
}

// 方括号里是 token 序列，不是表达式；同时它落在一条**可靠展开**的区间内。
var value: Int64 = @Foo[1+](2+3)
