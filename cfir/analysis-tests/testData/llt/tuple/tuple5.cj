// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// ASSERT: regex-auto tuple index must be in bounds

main(): Int64 {
    var ((a, b), c): ((Int32, Int32), Int32) = ((0, 1), 2)
    var d = ((1, 2), (4, 5, 6))
    var e = d[0][0]
    var f = d[0][2]
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
