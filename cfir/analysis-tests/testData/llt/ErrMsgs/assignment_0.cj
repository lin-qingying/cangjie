// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt -c %n.cj 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

main(): Int64 {
    var x: C<T> = 1(true)
    var (a1, a2): (C<T1>, C<T2>) = (false, false)

    x = 1
    a1 = (false, false)
    a2 = false
    0
}

// ASSERT: scan-not initializer's type is incompatible with variable declaration
// ASSERT: scan-not :9:
// ASSERT: scan-not :10:
// ASSERT: scan-not :11: 
