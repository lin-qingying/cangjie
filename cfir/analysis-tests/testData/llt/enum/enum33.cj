// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum E1 {
    e | e2

    public func foo() {
        e2
    } // Choose E1.e2
}

extend E1 {
    public func goo() {
        e2
    } // Choose E1.e2
}

enum E2 {
    e(Int64) | e2

    public func foo() {
        <!ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS!>e<!>
    } // Choose E1.e
}

extend E2 {
    public func goo() {
        <!ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS!>e<!>
    } // Choose E1.e
}

enum E<T> {
    e(T) | e(T, T)

    public func x(i: T) {
        e(i);
        e(i, i)
    } // Choose e in E<T>
}

enum F<T> {
    e(T) | e(T, T)

    public func x(i: T) {
        e(i);
        e(i, i)
    } // Choose e in F<T>
}

extend<T> F<T> {
    public func xx(i: T) {
        e(i);
        e(i, i)
    } // Choose e in F<T>
}

main(): Int64 {
    0
}
