package authority.memberforward

class <caret_resDeclaration>Res <: Resource {
    public func isClosed(): Bool { false }
    public func close(): Unit {}
    public func <caret_memberDeclaration>next(): Res { <caret_memberBodyConstructor>Res() }
}

main() {
    try (<caret_resourceA>a = <caret_forwardC>c, <caret_resourceB>b = <caret_receiverA>a.<caret_memberUse>next(), <caret_resourceC>c = <caret_backwardB>b) {}
}
