// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-PIPE-0: %run %run_opt %output %run_args

interface I1 {
    static func foo() {
        1
    }
}

interface I2 <: I1 {
    static func foo1() {
        2
    }
}

enum A {
    | Red | Green | Blue
}

extend A <: I1 {}

extend A <: I2 {}

main(): Int64 {
    var res = A.foo()
    if (res != 1) {
        return 1
    }
    0
}
