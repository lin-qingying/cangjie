// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class Base {
    public func foo(a: Int32): Int32 {
        0
    }
    public static func foo2<X>(a: X): X {
        a
    }
    public static func <!sema_static_function_overload_conflicts!>foo<!>(a: Int64): Int64 {
        a
    }
}

class Sub <: Base {
    <!sema_static_and_non_static_member_cannot_have_same_name!>p<!>ublic static func foo(a: Int16): Int16 {
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
    xv(yv(<!sema_mismatched_types!>B<Int64>()<!>))
    return 0
}

main(): Int64 {
    var a = A()
    var i = a.<!sema_no_match_function_declaration_for_call!>f<!>(<!sema_ambiguous_func_ref!>a<!>.g, Base())
    var f2: ((Sub) -> Int64, Base) -> Int64 = a.f
    var i2 = f2(<!sema_ambiguous_func_ref!>a<!>.g, Base())
    test<Int64>(1)
    return 0
}
