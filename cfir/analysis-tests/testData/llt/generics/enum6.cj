// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

class B {}

enum A<T> {
    a(?T)
}

main(): Int64 {
    var x = a<B>(B())
    return 0
}
