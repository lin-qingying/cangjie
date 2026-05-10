// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo(a: A): Int32 {
    123
    return 1
    1.2
    return <!TYPE_MISMATCH!>"hello"<!>
}

class A {}

func bar(b: Int32): Int32 {
    123
    1
    <!TYPE_MISMATCH!>12.0<!>
}
