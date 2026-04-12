// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {
    func h1(): Int64
    func h2(): Int64 {
        h3() * 10
    }
    func h3(): Int64
    func h4(): Int64 {
        1
    }
}

struct C {}

extend C <: I {
    public func h1(): Int64 {
        h2() * 10
    }
    public func h3(): Int64 {
        h4() * 10
    }
}

main() {
    var c: C = C()
    return c.h1() + c.h2() + c.h3() + c.h4() - 1111
}
