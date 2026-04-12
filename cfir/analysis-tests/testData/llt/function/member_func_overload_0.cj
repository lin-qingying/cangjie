// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

open class Base {
    public open func foo(a: Int32): Int32 {
        0
    }
}

class Sub <: Base {
    public override func foo(a: Int32): Int32 {
        0
    }
}

struct B<T> {}

class A {
    public func f(a: (Base) -> Int64, b: Sub) {
        return a(Base()) + 1
    } //f1
    public func f(a: (Sub) -> Int64, b: Base) {
        return a(Sub()) + 2
    } //f2
    public func f3(a: (Base) -> Int64) {
        return a(Base())
    }
    public func g(a: Base): Int64 {
        return 1
    } //g1
    public func g(a: Sub): Int64 {
        return 2
    } //g2
}

func x<T>(input: B<T>): B<T> {
    input
}

func y<T>(input: B<T>): B<T> {
    input
}

func test<T>(input: T): Int64 {
    let xv = x<T>
    let yv = y<T>
    xv(yv(B<T>()))
    return 0
}

main(): Int64 {
    var a = A()
    var f3: ((Base) -> Int64) -> Int64 = a.f3
    var i = a.f(a.g, Sub())
    var i1 = f3(a.g)
    var i2 = test<Int64>(1)
    if (i != 2 || i1 != 1) {
        return 1
    }
    return 0
}
