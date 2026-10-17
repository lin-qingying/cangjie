import std.ast.*

func isIdentifier(kind: TokenKind): Bool {
    kind == IDENTIFIER
}

func isQualifiedIdentifier(kind: TokenKind): Bool {
    kind == TokenKind.IDENTIFIER
}

main() {}
