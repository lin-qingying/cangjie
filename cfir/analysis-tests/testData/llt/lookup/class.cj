// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
class A {
    public let a = () => {
        foo()
    }
    public func foo() {
        let b = c
    }
    public let c = 321
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
