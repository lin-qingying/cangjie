// MODULE: macros
// MODULE_KIND: Source
// FILE: macros.cj
macro package untitled89.b

public class Tokens {}

public macro a11(a1: Tokens): Tokens {
    return a1
}

// MODULE: main(macros)
// MODULE_KIND: Source
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
import untitled89.b.a11

func testLowercaseMacroImport(): Int64 {
    let value: Int64 = @a11(42)
    return value
}
