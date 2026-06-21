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

// FILE: device_info.cj


package ohos.device_info

public class DeviceInfo {
    public static prop sdkApiVersion: Int64 {
        get() {19}
    }
}

// FILE: test07.cj



import ohos.labels.*
import ohos.device_info.*

@!APILevel[since: "19"]
public func f19() {}

@!APILevel[since: "20"]
public func f20() {}

@!APILevel[since: "21"]
public func f21() {}

@!APILevel[since: "22"]
public func f22() {}

@!APILevel[since: "23"]
public func f23() {}

main() {
    @IfAvailable(level: 20, {=> 
        @IfAvailable(level: 22, {=> // <= 22
            f19() // ok
            f20() // ok
            f21() // ok
            f22() // ok
            <!APILEVEL_REF_HIGHER!>f23()<!> // error
        }, {=> // <= 20
            f19() // ok
            f20() // ok
            <!APILEVEL_REF_HIGHER!>f21()<!> // error
            <!APILEVEL_REF_HIGHER!>f22()<!> // error
            <!APILEVEL_REF_HIGHER!>f23()<!> // error
        }) 
    }, {=>
        @IfAvailable(level: 21, {=> // <= 21
            f19() // ok
            f20() // ok
            f21() // ok
            <!APILEVEL_REF_HIGHER!>f22()<!> // error
            <!APILEVEL_REF_HIGHER!>f23()<!> // error
        }, {=> // <= 20
            f19() // ok
            f20() // ok
            <!APILEVEL_REF_HIGHER!>f21()<!> // error
            <!APILEVEL_REF_HIGHER!>f22()<!> // error
            <!APILEVEL_REF_HIGHER!>f23()<!> // error
        })
    })
}
