// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f
// (CJNATIVE)ASSERT: scan I2 foo
// (not CJNATIVE)ASSERT: scan I1 foo

interface I1 {
    func foo() {
        println("I1 foo")
    }
}

interface I2 <: I1 {
    func foo() {
        println("I2 foo")
    }
}

class A {}

extend A <: I1 {}

extend A <: I2 {}

main() {
    A().foo() // I1 foo
}
