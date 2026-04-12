// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I<M> {
    static func test<T>(a: T): M
}

class Base <: I<Base> {
    var a = 111
    public static func test<T>(a: T): Base {
        let b = Base()
        b.a = 0
        return b
    }
}

class A {
    static func foo<T>(a: T): T where T <: I<T> {
        var b: T = T.test(a)
        return b
    }
}

main(): Int64 {
    let ins = A.foo<Base>(Base())
    return ins.a
}
