// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %compile_lib_opt %n.cj 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

main(): Int64 {
    let x: C<T> = 1(true)
    let y = 1(true)
    let z: Bool = 1

    let (a1, a2): (C<T1>, C<T2>) = (false, false)
    let (b1, b2) = false
    let (c1, c2): (Int64, Bool) = (true, false)

    0
}

// ASSERT: scan-not initializer's type is incompatible with variable declaration
// ASSERT: regex-1 initializer isn't a tuple to match tuple pattern
