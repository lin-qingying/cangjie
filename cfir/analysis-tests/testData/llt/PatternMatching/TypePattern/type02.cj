// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

open class Father {}

class Son <: Father {
    public var one: Int64 = 2
}

main(): Int64 {
    var father = Father()
    var res = match (father) {
        case son: Son => son.one
        case _ => 1
    }
    return Int64(res - 1)
}
