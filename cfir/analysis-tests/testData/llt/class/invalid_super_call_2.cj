// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class B {}

class A <: B {
    public var a = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>()
}

main() {
    A()
    C()
    0
}

class C {
    public var a = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>()
}
