// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args | compare %f
// ASSERT: scan-2 114514

interface I3<T> {
    func i3foo(f: Float64): T
}

interface I4<T> {
    func i4foo(f: Float64): T
}

class BBB {
    public func goo(): Int64 {
        0i64
    }
}

abstract class A0 <: A3 & I3<BBB> & I4<BBB> {
    public open func i3foo(f: Float64): BBB

    public open func i4foo(f: Float64): BBB
}

abstract class A1 <: A3 {
    public open func i3foo(f: Float64): BBB

    public open func i4foo(f: Float64): BBB
}

abstract class A2 <: A1 & I3<BBB> {
    public open func i3foo(f: Float64): BBB

    public open func i4foo(f: Float64): BBB
}

abstract class A3 <: I3<BBB> {
    public open func i3foo(f: Float64): BBB

    public open func i4foo(f: Float64): BBB
}

abstract class A4 <: A3 & I3<BBB> {
    public open func i3foo(f: Float64): BBB

    public open func i4foo(f: Float64): BBB
}

open class O1 <: A0 & I4<BBB> {
    let O1a = 114514i64

    public open func i4foo(f: Float64): BBB {
        println(this.O1a)
        BBB()
    }

    public open func i3foo(f: Float64): BBB {
        println(this.O1a)
        BBB()
    }
}

main(): Unit {
    let x1: A0 = O1()
    x1.i3foo(114.514000f64)
    x1.i4foo(114.514000f64)
}
