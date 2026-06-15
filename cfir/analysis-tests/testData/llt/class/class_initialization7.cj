// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    let x: A
    public A(x: A) {
        this.x = x
    }
}

class B <: A {
    public B() {
        super(this.<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>x<!>)
    }
}

main() {
    let b = B()
    let a = b.x
    let aa = a.x
    return 0
}
