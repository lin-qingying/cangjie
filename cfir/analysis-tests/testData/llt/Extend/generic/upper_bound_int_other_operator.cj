// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {}

extend Int8 <: I {}

class C<T> where T <: I {
    func f(a: Int8) {
        var b = a
        b += 1
        b++
        return b
    }
}

main() {
    var c = C<Int8>()
    print(c.f(1))
    if (c.f(1) != 3) {
        return 1
    }
    return 0
}
