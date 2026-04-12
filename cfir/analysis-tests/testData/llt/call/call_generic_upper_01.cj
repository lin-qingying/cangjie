// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I3<T> {
    func foo(a: T): T {
        return a
    }
}

class C<T, V> <: I3<T> & I3<V> {}

class D <: I3<Int32> & I3<Rune> {}

main() {
    let c = C<Int32, String>()
    if (c.foo(11) != 11) {
        return 1
    }
    if (c.foo("str") != "str") {
        return 2
    }

    let d = D()
    if (d.foo(11) != 11) {
        return 3
    }
    if (d.foo(r'c') != r'c') {
        return 4
    }
    return 0
}
