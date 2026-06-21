

import std.ast.*
import std.deriving.*

@Annotation
public class APILevel {
    public const init() {}
}

func foo() {
    let tokens = quote(
    public enum Battery {
        @APILevel
        NONE
        | ENABLE
    }
    )
    let decl = parseDecl(tokens)
    let e = (decl as EnumDecl).getOrThrow()
    println(e.toTokens().toString())
}

main() {
    foo()
    0
}
