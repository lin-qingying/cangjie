// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output | compare %f

interface I<T> {
    func f1(a: T): Unit {
        println("I")
    }
    func f2(a: T): Unit {
        f1(a)
    }
}

open class A<T, K> {
    public func f1(a: T): Unit {
        println("a")
    }
    public func f1(a: K): Unit {
        println("b")
    }
}

class B <: A<Int64, String> & I<Int64> & I<String> {}

class C<T, K> <: A<T, K> & I<K> & I<T> {}

main() {
    var b = B()
    b.f2(1)
    b.f2("1")
    var c = C<Int64, String>()
    c.f2(1)
    c.f2("1")
}

/* SCAN
a
b
a
b
*/
