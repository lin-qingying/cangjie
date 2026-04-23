// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    func foo() {
        this.<!NOT_MEMBER_OF!>bar<!>() // error
        <!UNRESOLVED_REFERENCE!>bar<!>() // error
    }
}

extend A {
    func bar() {}
}

struct S {
    func foo() {
        this.<!NOT_MEMBER_OF!>bar<!>() // error
        <!UNRESOLVED_REFERENCE!>bar<!>() // error
    }
}

extend S {
    func bar() {}
}

enum E {
    EE

    func foo() {
        this.<!NOT_MEMBER_OF!>bar<!>() // error
        <!UNRESOLVED_REFERENCE!>bar<!>() // error
    }
}

extend E {
    func bar() {}
}

main() {}
