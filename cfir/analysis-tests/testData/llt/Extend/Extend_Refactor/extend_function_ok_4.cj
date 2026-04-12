// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

interface I1 {
    func foo(): Int64 {
        0
    }
}

interface I2 <: I1 {
    func foo(): Int64 {
        1
    }
}

class C1 {}

extend C1 <: I2 { // ok

}

main() {
    return 0
}
