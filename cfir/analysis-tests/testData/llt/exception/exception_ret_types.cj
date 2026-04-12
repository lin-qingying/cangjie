// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I1 {}

interface I2 {}

class C1 <: I1 & I2 {}

class C2 <: I1 & I2 {}

func foo(): Int64 {
    try {
        C1()
    } catch (_) {
        C2()
    } finally {
        "A Bad Type"
    }
    return 0
}

main() {
    return foo()
}
