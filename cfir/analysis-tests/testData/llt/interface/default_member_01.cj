// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I<T> {
    func test(v: T): Unit {
        let x = A<T>(v)
        let y = x.goo()
        x.foo(y)
    }
}

class A<T> <: I<T> {
    let v: T
    init(a: T) {
        v = a
    }
    func foo(a: T): Unit {}
    func goo(): T {
        v
    }
}

main(): Int64 {
    let ins = A<Int64>(1)
    ins.test(2)
    0
}
