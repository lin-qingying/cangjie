// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    ~init() {}
}

class B {
    ~init() {}
}

class C {
    ~init<!CANNOT_CURRYING!>(<!>)() {}
}

class D {
    ~init() {}
    <!CONFLICTING_OVERLOADS!>~init<!>() {}
}

open class E {
    <!FINALIZER_FORBIDDEN_IN_CLASS!>~<!>init() {}
}

open class F {
    <!FINALIZER_FORBIDDEN_IN_CLASS!>~<!>init() {}
    public open func f() {
        0
    }
}

open class Parent {
    public open func f() {
        0
    }
}

class Children {
    ~init() {}
}

main() {
    0
}
