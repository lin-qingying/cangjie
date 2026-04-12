// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

public interface PermissionValue {
    operator func &(rhs: PermissionValue): PermissionValue
    operator func |(rhs: PermissionValue): PermissionValue
}

public class PermissionAnd <: PermissionValue {
    public const init(lhs_: PermissionValue, rhs_: PermissionValue) {
        lhs = lhs_
        rhs = rhs_
    }
    public operator const override func &(rhs: PermissionValue): PermissionValue {
        return PermissionAnd(this, rhs)
    }
    public operator const override func |(rhs: PermissionValue): PermissionValue {
        return PermissionOr(this, rhs)
    }

    public let lhs: PermissionValue
    public let rhs: PermissionValue
}

public class PermissionOr <: PermissionValue {
    public const init(lhs_: PermissionValue, rhs_: PermissionValue) {
        lhs = lhs_
        rhs = rhs_
    }
    public operator const override func &(rhs: PermissionValue): PermissionValue {
        return PermissionAnd(this, rhs)
    }
    public operator const override func |(rhs: PermissionValue): PermissionValue {
        return PermissionOr(this, rhs)
    }
    public let lhs: PermissionValue
    public let rhs: PermissionValue
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
    // since
    public let level: UInt8
    public let permission: ?PermissionValue
    public const init(level_val: UInt8, permission!: ?PermissionValue = None) {
        this.level = level_val
        this.permission = permission
    }
}

class C {
    @APILevel[10, permission: "hah" | "world"]
    func foo() {
        1
    }
}

main() {
}
