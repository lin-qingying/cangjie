package completion.sections

macro Foo(attrTokens: Tokens, inputTokens: Tokens): Tokens {
    attrTokens
}

// 方括号里是 token 序列，不是表达式：补全在这里应当一个语义候选都不给。
var value: Int64 = @Foo[1+](2+3)
