// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj

internal import pkg.*

public class B <: A {
    public func foo<T>(a: T) {
        return 2
    }
    public func test(): Int64 {
        2
    }
}

extend B <: I1 {}

func test<T>(ins: T) where T <: I1 {
    ins.test()
}

main(): Int64 {
    let a = B()
    if (a.foo(Sub()) != 1) {
        return 1
    }
    if (test(a) != 2) {
        return 2
    }
    return 0
}
