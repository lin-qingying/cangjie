// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg/pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt %n.cj pkg.%middle -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import pkg.*

struct R {
    public func h1(): Int64 {
        1
    }
}

extend R <: I {
    public func h2(): Int64 {
        10
    }
}

class C {
    public func h1(): Int64 {
        2
    }
}

extend C <: I {
    public func h2(): Int64 {
        20
    }
}

enum E {
    a

    public func h1(): Int64 {
        3
    }
}

extend E <: I {
    public func h2(): Int64 {
        30
    }
}

main(): Int64 {
    var r = R()
    if (r.p != 11) {
        return 1
    }
    var c = C()
    if (c.p != 22) {
        return 2
    }
    var e = E.a
    if (e.p != 33) {
        return 3
    }
    return 0
}
