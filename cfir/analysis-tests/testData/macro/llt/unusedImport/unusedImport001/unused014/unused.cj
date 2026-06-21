// FILE: macro.cj
macro package macro_definition

import std.ast.*
public macro ModifyFunc(input: Tokens): Tokens {
    return input
}



// FILE: unused.cj
import macro_definition

@macro_definition.ModifyFunc func testA() {
}
main() {}


