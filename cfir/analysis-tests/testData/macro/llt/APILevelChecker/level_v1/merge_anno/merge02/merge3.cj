// FILE: ../../apilevel.cj


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

// FILE: dep2.cj


package dep2

public let cccc: VArray<Int64, $5> = [1, 2, 3, 4, 5]
public let (aaaa, bbbb) = (1, 2)

// FILE: merge3.cj



import dep2.*

main() {
   println(<!APILEVEL_REF_HIGHER!>aaaa<!>)
   println(<!APILEVEL_REF_HIGHER!>bbbb<!>)
   let a = <!APILEVEL_REF_HIGHER!>cccc<!>
}
