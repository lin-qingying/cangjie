// FILE: ../level_syscap_check/apilevel.cj


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

// FILE: device_info.cj
package ohos.device_info

public class DeviceInfo {
    public static prop sdkApiVersion: Int64 {
        get() {18}
    }

}




// FILE: dep.cj
package dep

import ohos.labels.APILevel

@!APILevel[since: "19"]
public func func1(a!: Int64 = 1) {println(19)}

@!APILevel[since: "18"]
public func func2() {println(18)}

@!APILevel[since: "19"]
public class A {
	@!APILevel[since: "19"]
	public prop a: Int64 {
		get() {1}
	}
}





// FILE: dep1.cj
package dep

import ohos.labels.APILevel

@!APILevel[since: "18"]
public func func2() {println(18)}



// FILE: external_weak.cj
import dep.*
import ohos.device_info.DeviceInfo

main() {
    @IfAvailable(level: 19, {=> A().a; func1() }, {=> func2()})
    0
}
