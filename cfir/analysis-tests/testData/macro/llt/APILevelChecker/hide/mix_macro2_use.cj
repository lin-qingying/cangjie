// API_LEVEL: 20

// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: ../level/apilevel.cj


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

// FILE: mix_macro_dep.cj


macro package m

import std.ast.*

public macro M(input: Tokens) {
   return input
}

// FILE: mix_macro_dep1.cj


package anno

@Annotation
public class CustomAnno {
   public const init() {}
}

// FILE: mix_macro2.cj



package ohos.dep

import ohos.labels.*
import m.*
import anno.*

@M
@!Hide[isChecked: true]
public open class A {
}

@CustomAnno
@!Hide[isChecked: true]
public open class B {
}

@!APILevel[since: "21"]
@!Hide[isChecked: true]
public open class C {
}

@!Hide[isChecked: true]
@!APILevel[since: "21"]
public open class D {
}

@!Hide[isChecked: true]
@C
public struct E {
}

// FILE: mix_macro2_use.cj



import ohos.dep.*

main() {
    <!UNRESOLVED_REFERENCE!>A()<!> // undefined
    <!UNRESOLVED_REFERENCE!>B()<!> // undefined
    <!UNRESOLVED_REFERENCE!>C()<!> // undefined
    <!UNRESOLVED_REFERENCE!>D()<!> // undefined
    0
}
