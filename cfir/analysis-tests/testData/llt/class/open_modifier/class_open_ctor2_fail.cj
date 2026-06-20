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
            =>
            <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>f<!>oo()
            <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>g<!>oo()
            let f1 = <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>f<!>oo
            <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>g<!>oo
            this.<!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>c<!>oo
        }
    }
}

main(): Int64 {
    0
}
