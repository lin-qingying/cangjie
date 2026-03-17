class AccessControlMemberModifiers {
    private let privateValue: Int64 = 1
    protected var protectedValue: Int64 = 2
    internal let internalValue: Int64 = 3
    let implicitValue: Int64 = 4

    private func privateFn(): Unit {}
    protected func protectedFn(): Unit {}
    internal func internalFn(): Unit {}
    func implicitFn(): Unit {}
}
