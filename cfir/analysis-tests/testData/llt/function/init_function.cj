// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %f 2>&1 | compare %f
// ASSERT: regex-not core dump
// ASSERT: scan unexpected constructor in 'top-level' scope

class A {
    public init () {}
}

struct B {
    public init() {}
}

init () {} 

main() {0}
