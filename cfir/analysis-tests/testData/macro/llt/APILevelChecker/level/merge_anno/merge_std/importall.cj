// API_LEVEL: 11

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

// FILE: importall.cj



import std.argopt.*
import <!UNUSED_IMPORT!>std.ast.*<!>
import <!UNUSED_IMPORT!>std.binary.*<!>
import <!UNUSED_IMPORT!>std.collection.*<!>
import <!UNUSED_IMPORT!>std.collection.concurrent.*<!>
import <!UNUSED_IMPORT!>std.console.*<!>
import <!UNUSED_IMPORT!>std.convert.*<!>
import <!UNUSED_IMPORT!>std.core.*<!>
import <!UNUSED_IMPORT!>std.crypto.cipher.*<!>
import <!UNUSED_IMPORT!>std.crypto.digest.*<!>
import <!UNUSED_IMPORT!>std.database.sql.*<!>
import <!UNUSED_IMPORT!>std.deriving.*<!>
import <!UNUSED_IMPORT!>std.env.*<!>
import <!UNUSED_IMPORT!>std.fs.*<!>
import <!UNUSED_IMPORT!>std.io.*<!>
import <!UNUSED_IMPORT!>std.math.*<!>
import <!UNUSED_IMPORT!>std.math.numeric.*<!>
import <!UNUSED_IMPORT!>std.net.*<!>
import <!UNUSED_IMPORT!>std.objectpool.*<!>
import <!UNUSED_IMPORT!>std.overflow.*<!>
import <!UNUSED_IMPORT!>std.posix.*<!>
import <!UNUSED_IMPORT!>std.process.*<!>
import <!UNUSED_IMPORT!>std.random.*<!>
import <!UNUSED_IMPORT!>std.ref.*<!>
import <!UNUSED_IMPORT!>std.reflect.*<!>
import <!UNUSED_IMPORT!>std.regex.*<!>
import <!UNUSED_IMPORT!>std.runtime.*<!>
import <!UNUSED_IMPORT!>std.sort.*<!>
import <!UNUSED_IMPORT!>std.sync.*<!>
import <!UNUSED_IMPORT!>std.time.*<!>
import <!UNUSED_IMPORT!>std.unicode.*<!>
import <!UNUSED_IMPORT!>std.unittest.*<!>
import <!UNUSED_IMPORT!>std.unittest.common.*<!>
import <!UNUSED_IMPORT!>std.unittest.diff.*<!>
import <!UNUSED_IMPORT!>std.unittest.mock.*<!>
import <!UNUSED_IMPORT!>std.unittest.mock.mockmacro.*<!>
import <!UNUSED_IMPORT!>std.unittest.prop_test.*<!>
import <!UNUSED_IMPORT!>std.unittest.testmacro.*<!>

main() {
    <!APILEVEL_REF_HIGHER!><!DEPRECATED_WARNING!>ArgOpt<!>()<!>
    0
}
