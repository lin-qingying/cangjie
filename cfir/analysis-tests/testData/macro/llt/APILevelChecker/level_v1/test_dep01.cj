// FILE: apilevel.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

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

// FILE: dep_ok.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package dep

import ohos.labels.*

@!APILevel[9]
public func f9(): Unit {
}

@!APILevel[11]
public func f11(): Unit {
}

// 没写，相当于 @!APILevel[0]
public func f0(): Unit {
}

// FILE: test_dep01.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import dep.*

main() {
    f9()
    <!APILEVEL_REF_HIGHER!>f11()<!>
    f0()
}
