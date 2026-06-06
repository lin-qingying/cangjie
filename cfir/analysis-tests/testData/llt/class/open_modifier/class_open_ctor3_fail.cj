// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

interface I {
    func coo() {}
}

open class A <: I {
    protected func foo() {}
}

extend A {
    func goo() {}
}

open class B <: A {
    public init() {
        { // lambda
            => <!sema_use_this_as_an_expression_in_func!>t<!>his
        }
        let v = <!sema_use_this_as_an_expression_in_func!>t<!>his
        test(<!sema_use_this_as_an_expression_in_func!>t<!>his)
    }
}

func test(v: B) {
    1
}

main(): Int64 {
    0
}
