// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C {}

public class A<T> {}

public var <!ACCESSIBILITY_WITH_MAIN_HINT!>a<!> = C()
public var <!ACCESSIBILITY_WITH_MAIN_HINT!>a1<!> = (C(), C())
public var <!ACCESSIBILITY_ERROR!>b<!>: C = C()

public var (<!ACCESSIBILITY_WITH_MAIN_HINT!>a2<!>, <!ACCESSIBILITY_WITH_MAIN_HINT!>b2<!>): (C, C) = (C(), C())

public var (<!ACCESSIBILITY_WITH_MAIN_HINT!>a3<!>, <!ACCESSIBILITY_WITH_MAIN_HINT!>b3<!>) = (C(), C())

public func <!ACCESSIBILITY_WITH_MAIN_HINT!>foo<!>() {
    C()
}

public func <!ACCESSIBILITY_ERROR!>foo1<!>(): C {
    C()
}

public func <!ACCESSIBILITY_WITH_MAIN_HINT!>foo2<!>() {
    A<C>()
}

public func <!ACCESSIBILITY_ERROR!>foo3<!>(): A<C> {
    A<C>()
}

main() {
    return 0
}
