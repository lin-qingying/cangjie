// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

class A {
    public init(a: Int64, b: Int64, c: Int64) {
        while (a > 5) {
            if (b > 5) {
                if (c < 5) {
                    return A(a, b, c)
                }
            }
        }
    }
}

// ASSERT: scan expected 'Unit', found 'Class-A'
