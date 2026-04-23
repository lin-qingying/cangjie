// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A<T> {}

extend<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> A<T> where T <: Rune {
    func foo() {
        123
    }
}

class B <: A<Int64> {}

main() {
    let b = B()
    if (b.<!NOT_MEMBER_OF!>foo<!>() != 123) { // cannot access foo
        return 1
    }
    return 0
}
