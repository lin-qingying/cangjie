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
        h1()
    }
}

enum C {
    a
}

extend C <: I {
    public func h1(): Int64 {
        1
    }
}

main() {
    var c: C = C.a
    return c.h2() - 1
}
