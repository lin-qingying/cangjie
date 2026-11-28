// 访问控制：private / protected / internal / public 修饰成员与默认可见性。
// 官方依据：Cangjie 文档《访问控制》；cjc 1.0.5 / 1.1.3 零诊断。
open class AccessControl {
    private let privateValue: Int64 = 1
    protected var protectedValue: Int64 = 2
    internal let internalValue: Int64 = 3
    public let publicValue: Int64 = 4
    let implicitValue: Int64 = 5

    private func privateFn(): Unit {}
    protected func protectedFn(): Unit {}
    internal func internalFn(): Unit {}
    public func publicFn(): Unit {}
    func implicitFn(): Unit {}
}

class DerivedAccess <: AccessControl {
    public func use(): Int64 {
        return protectedValue + internalValue + publicValue + implicitValue
    }
}
