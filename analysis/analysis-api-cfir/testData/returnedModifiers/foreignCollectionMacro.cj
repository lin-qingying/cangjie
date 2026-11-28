// LANGUAGE_VERSION: 1.1.3

macro package returned_modifiers.macros

import std.ast.Tokens

// FOREIGN 是宏声明的非法 target；官方仍返回可映射的 FOREIGN 属性。
foreign {
    public macro inheritedMacro(input: Tokens): Tokens {
        return input
    }
}

public macro ordinaryMacro(input: Tokens): Tokens {
    return input
}
