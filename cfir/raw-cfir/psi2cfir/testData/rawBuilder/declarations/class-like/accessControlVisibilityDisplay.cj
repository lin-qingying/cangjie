// RUN_PIPELINE_TILL: FRONTEND
internal class ExplicitInternal {
    internal func explicitInternal(): Unit {}
    func implicitInternal(): Unit {}
    private func privateVisible(): Unit {}
}

class ImplicitInternal {
    internal func explicitInternalMember(): Unit {}
    func implicitInternalMember(): Unit {}
}

