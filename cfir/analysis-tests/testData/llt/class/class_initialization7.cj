// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %diag_json %f 2>&1 | compare %f
// ASSERT: scan sema_assignment_of_member_variable_cannot_use_this_or_super
// ASSERT: scan "Errors": 1,

open class A {
    let x: A
    public A(x: A) {
        this.x = x
    }
}

class B <: A {
    public B() {
        super(this.x)
    }
}

main() {
    let b = B()
    let a = b.x
    let aa = a.x
    return 0
}