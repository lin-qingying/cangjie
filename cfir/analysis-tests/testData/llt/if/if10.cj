// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class Object {}

class A <: Object {}

class B <: Object {}

main() {
    let a = if (true) {
        A()
    } else {
        B()
    }
    0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
