package authority.resources

class <caret_resDeclaration>Res <: Resource {
    public func isClosed(): Bool { false }
    public func close(): Unit {}
}

main() {
    let <caret_outerDeclaration>b = <caret_outerConstructor>Res()
    try (<caret_resourceB>b = <caret_initializerOuter>b) {
        let <caret_valueDeclaration>value: Res = <caret_bodyResource>b
    }
}
