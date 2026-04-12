// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<T> {
    func test(a: T) {
        return a
    }
}

interface I2<K, V> <: I<K> & I<V> {}

interface I3<M, P, Q> <: I2<(M, P), (P, Q)> & I2<M, P> {}

class A <: I3<Rune, Int64, Bool> & I3<String, Bool, Unit> {
    func foo() {
        0
    }
}

main() {
    let a = A()
    a.foo()
    let v1 = a.test(r'a')
    if (v1 != r'a') {
        return 1
    }
    let v2 = a.test(11)
    if (v2 != 11) {
        return 2
    }
    let v3 = a.test("str")
    if (v3 != "str") {
        return 3
    }
    let v4 = a.test(true)
    if (!v4) {
        return 4
    }
    let v5 = a.test((r'c', 1))
    if (v5[0] != r'c' || v5[1] != 1) {
        return 5
    }
    let v6 = a.test((1, false))
    if (v6[0] != 1 || v6[1]) {
        return 6
    }
    return 0
}
