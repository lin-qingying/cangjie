// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


abstract class A {
    var b: B
    init() {
        b = B()
        b.bind(<!sema_use_this_as_an_expression_in_func!>t<!>his)
    }
}

class B {
    var a: Option<A> = Option<A>.None
    public func bind(a: A) {
        this.a = a
    }
}

main(): Int64 {
    0
}
