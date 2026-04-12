// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I1 {
    func foo2() {
        11
    }
}

interface I2 <: I1 {
    func foo2() {
        0
    }
}

interface I3 {}

extend Int64 <: I2 & I3 {}

main() {
    return 1.foo2()
}
