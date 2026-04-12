// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

struct A {}

interface I1 {
    static func g() {}
}

extend A <: I1 {}

interface I2 {}

extend A <: I2 {
    static func f() {
        return g()
    }
}

main() {
    return 0
}
