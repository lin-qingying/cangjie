// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I1<T> {
    static func test<T1>(a: T1): Bool {
        return false
    }
}

interface I2<T2> <: I1<T2> where T2 <: I1<T2> {
    static func test<T3>(a: T3): Bool {
        return true
    }
}

class A<T4> where T4 <: I2<T4> {
    func test(val: T4): Bool {
        return T4.test(val) // Test for this type check
    }
}

class B <: I2<B> {}

main(): Int64 {
    let a = A<B>()
    if (a.test(B())) {
        return 0
    }
    return 1
}
