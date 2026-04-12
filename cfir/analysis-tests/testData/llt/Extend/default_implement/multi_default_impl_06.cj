// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-PIPE-0: %run %output | compare %f
// (CJNATIVE) ASSERT: scan-2 2
// (not CJNATIVE) ASSERT: scan-2 1

interface I1 {
    func foo() {
        1
    }
}

interface I2 <: I1 {
    func foo() {
        2
    }
}

class A {}

extend A <: I2 {}

extend A <: I1 {}

class B {}
// Diff order.
extend B <: I1 {}

extend B <: I2 {}

main() {
    println(A().foo())
    println(B().foo())
}
