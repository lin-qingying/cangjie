// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %cmp_opt %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

class A<T> {
    public func f(a!: A<T> = f()): A<T> {
        return A<T>()
    }
}
