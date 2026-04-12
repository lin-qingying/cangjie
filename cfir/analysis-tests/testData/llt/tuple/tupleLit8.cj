// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main(): Int64 {
    var a: (Int64, Int64) = (1, 1)
    if (true) {
        a
    } else {
        (1, 1)
    }
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
