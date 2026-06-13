// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    public var a = 1
}

open class B <: A {
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic var a = 10
}

class C <: B {
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic var a = 20
}

main() {
    return 1
}
