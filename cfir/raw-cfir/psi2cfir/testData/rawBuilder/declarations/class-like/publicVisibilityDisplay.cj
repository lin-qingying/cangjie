// STUB_FALLBACK_TO_AST:
public class ExplicitPublic {
    public func explicitPublic(): Unit {}
    func implicitInternal(): Unit {}

    public let explicitPublicValue: Int64 = 1
    let implicitInternalValue: Int64 = 2
}

class ImplicitInternalFromClassDefault {
    public func publicMember(): Unit {}
}
