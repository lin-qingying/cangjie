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

var x = 1

struct C {}

extend C <: I {
    public func f2() {
        f1() * 10
    }
    public func f4() {
        f3() * 10
    }
}

main(): Int64 {
    var c = C()
    return c.f1() + c.f2() + c.f3() + c.f4() - 2222
}
