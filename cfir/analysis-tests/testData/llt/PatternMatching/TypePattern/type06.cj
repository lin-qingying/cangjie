// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

open class Father {
    public var age = 40
}

class Son <: Father {
    public var one: Int32 = 2
    public init() {
        age = 20
    }
}

class Uncle {}

enum Family {
    son(Son)
}

main(): Int64 {
    var target = Family.son(Son())
    var res = match (target) {
        case son(uncle: Uncle) => 0
        case son(father: Father) => father.age
        case _ => -1
    }
    if (res == 20) {
        return 0
    }
    return 1
}
