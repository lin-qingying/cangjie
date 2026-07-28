// API_LEVEL_SYSCAP: json/config.json

// FILE: apilevel.cj


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
    public let crossspecific: Bool
    public let deprecated: ?String
    public let form: Bool
    public let permission: ?PermissionValue
    public let syscap: String
    public let throwexception: Bool
    public let workerthread: Bool
    public let systemapi: Bool
    public const init(since!: String, atomicservice!: Bool = false, crossspecific!: Bool = false,
        deprecated!: ?String = None, form!: Bool = false, permission!: ?PermissionValue = None,
        syscap!: String = "", throwexception!: Bool = false, workerthread!: Bool = false, systemapi!: Bool = false) {
        this.since = since
        this.atomicservice = atomicservice
        this.crossspecific = crossspecific
        this.deprecated = deprecated
        this.form = form
        this.permission = permission
        this.syscap = syscap
        this.throwexception = throwexception
        this.workerthread = workerthread
        this.systemapi = systemapi
    }
}

// FILE: canIUse.cj


package ohos.base

public func canIUse(syscap: String): Bool {
    return syscap == "camera"
}

// FILE: syscap_test01.cj



// Note: 2in1:   [A, B] + [A, C] ===> [A, B, C]
//       phone:  [A, D] + [A, E] ===> [A, D, E]
//       tablet: [A, F] + [A, G] ===> [A, F, G]
// intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]

import ohos.labels.*
import ohos.base.*

@!APILevel[since: "19", syscap: "A"]
public func f_A() {}

@!APILevel[since: "19", syscap: "B"]
public func f_B() {}

@!APILevel[since: "19", syscap: "C"]
public func f_C() {}

@!APILevel[since: "19", syscap: "D"]
public func f_D() {}

@!APILevel[since: "19", syscap: "E"]
public func f_E() {}

@!APILevel[since: "19", syscap: "F"]
public func f_F() {}

@!APILevel[since: "19", syscap: "G"]
public func f_G() {}

@!APILevel[since: "19", syscap: "H"]
public func f_H() {}

@!APILevel[since: "19", syscap: "I"]
public func f_I() {}

main() {
    @IfAvailable(syscap: "H", {=> // intersectionSet: [A, H], unionSet: [A, B, C, D, E, F, G, H]
        f_A() // ok
        <!APILEVEL_SYSCAP_WARNING!>f_B()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        f_H() // ok
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    }, {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        <!APILEVEL_SYSCAP_WARNING!>f_B()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        <!APILEVEL_SYSCAP_ERROR!>f_H()<!> // error
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    })

    @IfAvailable(syscap: "A", {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        <!APILEVEL_SYSCAP_WARNING!>f_B()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        <!APILEVEL_SYSCAP_ERROR!>f_H()<!> // error
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    }, {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        <!APILEVEL_SYSCAP_WARNING!>f_B()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        <!APILEVEL_SYSCAP_ERROR!>f_H()<!> // error
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    })

    @IfAvailable(syscap: "B", {=> // intersectionSet: [A, B], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        f_B() // ok
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        <!APILEVEL_SYSCAP_ERROR!>f_H()<!> // error
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    }, {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
        <!APILEVEL_SYSCAP_WARNING!>f_B()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_C()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_D()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_E()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_F()<!> // warning
        <!APILEVEL_SYSCAP_WARNING!>f_G()<!> // warning
        <!APILEVEL_SYSCAP_ERROR!>f_H()<!> // error
        <!APILEVEL_SYSCAP_ERROR!>f_I()<!> // error
    })
}
