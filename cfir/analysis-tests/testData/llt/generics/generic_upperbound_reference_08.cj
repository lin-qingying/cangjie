// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I<T> <: Equatable<T> where T <: Equatable<T> {
    operator func ==(a: T): Bool {
        true
    }
    operator func !=(a: T): Bool {
        false
    }
}

class A<T> where T <: I<T> {
    let a: T
    init(v: T) {
        a = v
    }
    func test(val: T): Bool {
        return val == a // Test for this type check
    }
}

class B <: I<B> {}

main(): Int64 {
    let b = B()
    let a = A<B>(b)
    a.test(b)
    return 0
}
