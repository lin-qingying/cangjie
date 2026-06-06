// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class A {
    public var a = 1
}

open class B <: A {
    <!sema_member_variable_can_not_shadow!>p<!>ublic var a = 10
}

class C <: B {
    <!sema_member_variable_can_not_shadow!>p<!>ublic var a = 20
}

main() {
    return 1
}
