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

struct C {}

extend C <: I {
    func g() {
        f() * 10
    }
}

main(): Int64 {
    var c = C()
    return c.f() + c.g() - 22
}
