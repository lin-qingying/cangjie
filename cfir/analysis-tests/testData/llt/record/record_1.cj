// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f 2>&1 | compare %f

struct B {
    public let c: Int64
    public let a: Int64 = 1
    public init(c: Int64, a: Int64) {
        this.c = c
        this.a = a
    }
}

main(): Int64 {
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
