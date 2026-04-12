// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// ASSERT: regex-auto cannot convert an integer literal to type 'Bool'

func foo(): Int32 {
    let arr1: Int32 = 0
    let arr2: Array<Int32> = [1, 2]
    for (a in arr1) {
    }
    for (a in arr2 where 10) {
    }
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
