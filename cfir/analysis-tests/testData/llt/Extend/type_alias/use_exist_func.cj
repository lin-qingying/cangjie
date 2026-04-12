// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Foo {
    public func f() {
        1
    }
}

type F = Foo

interface I {
    func f() {
        2
    }
}

type i = I

extend F <: i {}

main() {
    var a = F()
    if (a.f() != 1) {
        return 1
    }
    return 0
}
