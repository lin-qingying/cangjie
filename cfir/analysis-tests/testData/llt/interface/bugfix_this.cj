// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

var aa = 0

main(): Int64 {
    var b = C().f2(2)
    if (b is C && aa == 1 && b.a == 6) {
        return 0
    }
    return 1
}

abstract class Data {
    public Data(var a!: Int64 = 6) {
        aa++
    }

    public func f2(a: Int64): This {
        return this
    }

    public func f2(): This
}

class C <: Data {
    public func f2(): This {
        f2(2)
    }
}
