// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

interface I1 {
    prop p1: Int64 {
        get() {
            1
        }
    }
    static prop p2: Int64 {
        get() {
            1
        }
    }
    func f1() {
        1
    }
    static func f3() {
        1
    }
    static func f4<T>(v: T) {
        v
    }
}

open class C1 <: I1 {
    public prop p1: Int64 {
        get() {
            0
        }
    }
    public static prop p2: Int64 {
        get() {
            0
        }
    }
    public func f1() {
        0
    }
    public static func f3() {
        0
    }
    public static func f4<T>(v: T) {
        v
    }
}

class C2<M> <: C1 {}

class C<M> {
    var a: M
    init(x: M) {
        a = x
    }
}

interface A1<M> where M <: I1 {
    prop p1: Int64 {
        get() {
            1
        }
    }
    static prop p2: Int64 {
        get() {
            1
        }
    }
    func test(x1: M, x2: (M) -> M, x3: C<M>): M {
        if (p1 != 0) {
            throw Exception()
        }
        static_generic_test<Int64>(x1, x2, x3, 1).a
    }
    static func static_test(x1: M, x2: (M) -> M, x3: C<M>): (M) -> M {
        var v1: M = x1
        var v2 = x2
        var v3 = x3
        var v4 = v1 is Option<M>
        var v5 = v2 as Option<(M) -> M>
        var v6 = match (v3) {
            case _: Option<C<M>> => false
            case _: C<M> => true
        }
        var v7 = v1.p1 + M.p2 + v1.f1() + M.f3() + M.f4<Int64>(0)
        if (v4 || !v6 || v7 != 0 || p2 != 0) {
            throw Exception()
        }
        v2
    }
    static func static_generic_test<K>(x1: M, x2: (M) -> M, x3: C<M>, x4: K): C<M> {
        C<M>(static_test(x1, x2, x3)(x1))
    }
}

interface A2<M> <: A1<M> where M <: I1 {
    prop p1: Int64 {
        get() {
            0
        }
    }
    static prop p2: Int64 {
        get() {
            0
        }
    }
    func test(x1: M, x2: (M) -> M, x3: C<M>): M {
        if (p1 != 0) {
            throw Exception()
        }
        static_generic_test<Int64>(x1, x2, x3, 1).a
    }
    static func static_test(x1: M, x2: (M) -> M, x3: C<M>): (M) -> M {
        var v1: M = x1
        var v2 = x2
        var v3 = x3
        var v4 = v1 is Option<M>
        var v5 = v2 as Option<(M) -> M>
        var v6 = match (v3) {
            case _: Option<C<M>> => false
            case _: C<M> => true
        }
        var v7 = v1.p1 + M.p2 + v1.f1() + M.f3() + M.f4<Int64>(0)
        if (v4 || !v6 || v7 != 0 || p2 != 0) {
            throw Exception()
        }
        v2
    }
    static func static_generic_test<K>(x1: M, x2: (M) -> M, x3: C<M>, x4: K): C<M> {
        C<M>(static_test(x1, x2, x3)(x1))
    }
}

class A3<M> {}

extend<M> A3<M> <: A2<M> where M <: I1 {}

main() {
    var a = A3<C1>()
    var b = A3<C2<Int64>>()
    var v1 = a.test(C1(), {x: C1 => x}, C<C1>(C1()))
    var v4 = b.test(C2<Int64>(), {x: C2<Int64> => x}, C<C2<Int64>>(C2<Int64>()))
    if (v1 is C1 && v4 is C2<Int64>) {
        return 0
    }
    return 1
}
