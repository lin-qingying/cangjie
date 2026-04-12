// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// ASSERT: regex-auto at least one default case, such as wildcard pattern, variable pattern or '[...]' for sequence pattern in match case

main(): Int64 {
    var b: Int64 = match {
        case false => 0
    }
    return b
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
