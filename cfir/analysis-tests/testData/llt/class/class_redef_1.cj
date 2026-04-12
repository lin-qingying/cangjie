// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output %run_args

interface I1 {
    static func foo(): Int64
}

interface I2 {
    static func foo(): Int64 {
        return 1
    }
}

open class C1 {
    public static func foo(): Int64 {
        return 2
    }
}

open class C2 <: C1 & I1 & I2 {
    public static redef func foo(): Int64 {
        return 3
    }
}

class C3 <: C2 {}

class C4 <: I2 {}

main(): Int64 {
    var res1 = C1.foo()
    var res2 = C2.foo()
    var res3 = C3.foo()
    var res4 = C4.foo()
    if (res1 == 2 && res2 == 3 && res3 == 3 && res4 == 1) {
        return 0
    }
    return 1
}
