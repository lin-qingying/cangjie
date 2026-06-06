// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class A {
    ~init() {}
}

class B {
    ~init() {}
}

class C {
    ~init<!sema_cannot_currying!>(<!>)() {}
}

class D {
    ~init() {}
    <!sema_overload_conflicts!>~init<!>() {}
}

open class E {
    <!sema_finalizer_forbidden_in_class!>~<!>init() {}
}

open class F {
    <!sema_finalizer_forbidden_in_class!>~<!>init() {}
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
