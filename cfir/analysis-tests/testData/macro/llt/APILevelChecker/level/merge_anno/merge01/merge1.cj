// FILE: ../../apilevel.cj


package ohos.labels

public interface PermissionValue {
    operator func &(rhs: PermissionValue): PermissionValue
    operator func |(rhs: PermissionValue): PermissionValue
}

public class PermissionAnd <: PermissionValue {
    public let lhs: PermissionValue
    public let rhs: PermissionValue
    public const init(lhs: PermissionValue, rhs: PermissionValue) {
        this.lhs = lhs
        this.rhs = rhs
    }
    public operator const override func &(rhs: PermissionValue): PermissionValue {
        return PermissionAnd(this, rhs)
    }
    public operator const override func |(rhs: PermissionValue): PermissionValue {
        return PermissionOr(this, rhs)
    }
}

public class PermissionOr <: PermissionValue {
    public let lhs: PermissionValue
    public let rhs: PermissionValue
    public const init(lhs: PermissionValue, rhs: PermissionValue) {
        this.lhs = lhs
        this.rhs = rhs
    }
    public operator const override func &(rhs: PermissionValue): PermissionValue {
        return PermissionAnd(this, rhs)
    }
    public operator const override func |(rhs: PermissionValue): PermissionValue {
        return PermissionOr(this, rhs)
    }
}

extend String <: PermissionValue {
    public operator const func &(rhs: PermissionValue): PermissionValue {
        return PermissionAnd(this, rhs)
    }
    public operator const func |(rhs: PermissionValue): PermissionValue {
        return PermissionOr(this, rhs)
    }
}

@Annotation
public class APILevel {
    public let since: String
    public let atomicservice: Bool
    public let crossplatform: Bool
    public let deprecated: ?String
    public let form: Bool
    public let permission: ?PermissionValue
    public let syscap: String
    public let throwexception: Bool
    public let workerthread: Bool
    public let systemapi: Bool
    public const init(since!: String, atomicservice!: Bool = false, crossplatform!: Bool = false,
        deprecated!: ?String = None, form!: Bool = false, permission!: ?PermissionValue = None,
        syscap!: String = "", throwexception!: Bool = false, workerthread!: Bool = false, systemapi!: Bool = false) {
        this.since = since
        this.atomicservice = atomicservice
        this.crossplatform = crossplatform
        this.deprecated = deprecated
        this.form = form
        this.permission = permission
        this.syscap = syscap
        this.throwexception = throwexception
        this.workerthread = workerthread
        this.systemapi = systemapi
    }
}

// FILE: dep1.cj


package dep1

import ohos.labels.APILevel

public func foo(): Int64 {0}
public func foo(a: Array<Int64>): Int64 {1}
public func foo(a: Array<String>): Int64 {2}

public class A {
    public init() {}
    public init(a: Int64) {}
    public init(a: String) {}

    public func foo(): Int64 {0}
    public func foo(a: Array<Int64>): Int64 {1}
    public func foo(a: Array<String>): Int64 {2}

    public var a = 0
    public prop b: Int64 {
        get() {1}
    }
}

public struct B {
    public init() {}
    public init(a: Int64) {}
    public init(a: String) {}
    public func foo(): Int64 {0}
    public func foo(a: Array<Int64>): Int64 {1}
    public func foo(a: Array<String>): Int64 {2}

    public var a = 0
    public prop b: Int64 {
        get() {1}
    }
}

public enum C {
    A | B(Int64)

    public func foo(): Int64 {0}
    public func foo(a: Array<Int64>): Int64 {1}
    public func foo(a: Array<String>): Int64 {2}
}

public interface D {
    func foo(): Int64 {0}
    func foo(a: Array<Int64>): Int64 {1}
    func foo(a: Array<String>): Int64 {2}
}

// FILE: merge1.cj




import dep1.*

main() {
    foo() // level 1
    foo([1]) // level 2
    <!APILEVEL_REF_HIGHER!>foo(["1"])<!> // level 3
}
