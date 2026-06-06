// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

func test<T>(a: T) {}

main(): Int64 {
    test<Any> <!TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION!>{=> 1}<!>
    return 0
}
