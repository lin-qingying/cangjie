// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {
    func f(): Int64
}

class C {}

type c = C

extend c <: I {
    public func f() {
        2
    }
}

extend c {
    public func f1() {
        3
    }
}

type i64 = Int64

type i = I

extend i64 <: i {
    public func f() {
        4
    }
}

extend i64 {
    public func f1() {
        5
    }
}

main() {
    var a = c()
    if (a.f() != 2) {
        return 1
    }
    if (a.f1() != 3) {
        return 2
    }
    if (1.f() != 4) {
        return 3
    }
    if (2.f1() != 5) {
        return 4
    }
    var b: i64 = 3
    if (b.f() != 4) {
        return 5
    }
    if (b.f1() != 5) {
        return 6
    }
    return 0
}
