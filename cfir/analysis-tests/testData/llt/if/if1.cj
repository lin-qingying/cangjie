// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo(): Int32 {
    if (false) {
        return 2.6
    } else {
        var a: Int32 = 1
        return a
    }
    return 0
}

// ASSERT: scan cannot convert a floating-point literal to type 'Int32'
