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
    public var one: Int64 = 2
    public init() {
        age = 20
    }
}

main(): Int64 {
    var father = Father()
    var res = match (father) {
        case son: Son => son.one
        case _ => 1
    }
    if (res == 1) {
        return 0
    }
    return 1
}
