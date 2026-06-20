// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


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
            => <!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his
        }
        let v = <!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his
        test(<!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his)
    }
}

func test(v: B) {
    1
}

main(): Int64 {
    0
}
