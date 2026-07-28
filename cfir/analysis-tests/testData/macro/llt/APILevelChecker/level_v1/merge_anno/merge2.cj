// API_LEVEL: 2

// FILE: ../apilevel.cj


package ohos.labels

public interface PermissionValue {
    operator func &(rhs: PermissionValue): PermissionValue
    operator func |(rhs: PermissionValue): PermissionValue
}

public class PermissionAnd <: PermissionValue {
    public PermissionAnd(let lhs: PermissionValue, let rhs: PermissionValue) {}
    public operator override func &(rhs: PermissionValue): PermissionValue {
        return PermissionAnd(this, rhs)
    }
    public operator override func |(rhs: PermissionValue): PermissionValue {
        return PermissionOr(this, rhs)
    }
}

public class PermissionOr <: PermissionValue {
    public PermissionOr(let lhs: PermissionValue, let rhs: PermissionValue) {}
    public operator override func &(rhs: PermissionValue): PermissionValue {
        return PermissionAnd(this, rhs)
    }
    public operator override func |(rhs: PermissionValue): PermissionValue {
        return PermissionOr(this, rhs)
    }
}

extend String <: PermissionValue {
    public operator func &(rhs: PermissionValue): PermissionValue {
        return PermissionAnd(this, rhs)
    }
    public operator func |(rhs: PermissionValue): PermissionValue {
        return PermissionOr(this, rhs)
    }
}

@Annotation
public class APILevel {
    // since
    public let level: UInt8
    public let atomicservice: Bool
    public let crossplatform: Bool
    // deprecated: 0 means the Api is not deprecated
    public let deprecated: UInt8
    public let form: Bool
    public let permission: ?PermissionValue
    public let stagemodelonly: Bool
    public let syscap: String
    public const init(level_val: UInt8, atomicservice!: Bool = false, crossplatform!: Bool = false,
        deprecated!: UInt8 = 0, form!: Bool = false, permission!: ?PermissionValue = None, stagemodelonly!: Bool = true,
        syscap!: String = "") {
        this.level = level_val
        this.atomicservice = atomicservice
        this.crossplatform = crossplatform
        this.deprecated = deprecated
        this.form = form
        this.permission = permission
        this.stagemodelonly = stagemodelonly
        this.syscap = syscap
    }
}

// FILE: dep1.cj


package dep1

import ohos.labels.APILevel

public func foo(): Int64 {
    0
}

public func foo(a: Array<Int64>): Int64 {
    1
}

public func foo(a: Array<String>): Int64 {
    2
}

public class A {
    public init() {}
    public init(a: Int64) {}
    public init(a: String) {}

    public func foo(): Int64 {
        0
    }
    public func foo(a: Array<Int64>): Int64 {
        1
    }
    public func foo(a: Array<String>): Int64 {
        2
    }

    public var a = 0
    public prop b: Int64 {
        get() {
            1
        }
    }
}

public struct B {
    public init() {}
    public init(a: Int64) {}
    public init(a: String) {}
    public func foo(): Int64 {
        0
    }
    public func foo(a: Array<Int64>): Int64 {
        1
    }
    public func foo(a: Array<String>): Int64 {
        2
    }

    public var a = 0
    public prop b: Int64 {
        get() {
            1
        }
    }
}

public enum C {
    A | B(Int64)

    public func foo(): Int64 {
        0
    }
    public func foo(a: Array<Int64>): Int64 {
        1
    }
    public func foo(a: Array<String>): Int64 {
        2
    }
}

public interface D {
    func foo(): Int64 {
        0
    }
    func foo(a: Array<Int64>): Int64 {
        1
    }
    func foo(a: Array<String>): Int64 {
        2
    }
}

// FILE: merge2.cj



import dep1.*

main() {
    <!APILEVEL_REF_HIGHER!>A()<!>
    <!APILEVEL_REF_HIGHER!>B()<!>
    <!APILEVEL_REF_HIGHER!>C.A.foo()<!>
    <!APILEVEL_REF_HIGHER!>C.B(1).foo()<!>
}
