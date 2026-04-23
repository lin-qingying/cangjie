// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {}

extend<T> A<T> where T <: ToString {
    func f() {
        1
    }
}

extend<T> A<T> {
    func f2() {
        <!UNRESOLVED_REFERENCE!>f<!>
        this.<!NOT_MEMBER_OF!>f<!>
        this.<!NOT_MEMBER_OF!>f<!>()
    }
}

main() {}
