// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var varRune: Rune = '1'
    varRune = varRune << 1
    return 0
}

// ASSERT: regex-auto invalid binary operator '<<' on type 'Rune' and 'Int64'
// ASSERT: regex-not type incompatible in this assignment
