macro package semanticMember

import std.ast.*

public macro AddGeneratedMember(input: Tokens): Tokens {
    let funcDecl = parseDecl(quote(public func generatedMember(): Int64 { return 42 }))
    let decl = (parseDecl(input) as ClassDecl).getOrThrow()
    decl.body.decls.add(funcDecl)
    return decl.toTokens()
}
