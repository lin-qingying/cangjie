// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

struct R<T> {
    public static func r<U>(a: U): U {
        return a
    }
    public static func rr<K>(a: K) {
        return r<Bool>(true)
    }
}

open class A<K> {
    public static func a(x: K): Bool {
        return true
    }
    public static func b<U>(x: U, y: K) {
        return a(y)
    }
    public static func get<T>(x: T, y: K): Bool {
        return b<Rune>(r'a', y)
    }
}

interface I {
    static func i<T>(a: T): Int64 {
        return 1
    }
}

class Test <: I {}

main() {
    var i: Int64 = 1
    var j: Int64 = 2
    if (R<Int64>.r(i) != 1) {
        return 1
    }
    if (R<Int16>.rr<Rune>(r'a') != true) {
        return 1
    }
    if (A<Bool>.b(j, true) != true) {
        return 2
    }
    if (Test.i(i) != 1) {
        return 3
    }
    return 0
}
