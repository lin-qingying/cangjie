// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I1 {
    func f(): Int64
}

interface I2 <: I1 {
    func g(): Int64
}

class C {}

extend C <: I1 {
    public func f(): Int64 {
        1
    }
}

extend C <: I2 {
    public func g(): Int64 {
        2
    }
}

main() {
    var c = C()
    return c.f() + c.g() - 3
}
