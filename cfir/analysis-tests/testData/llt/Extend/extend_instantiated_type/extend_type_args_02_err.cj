// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {}

extend<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> A<T> where T <: String {
    func test() {
        2
    }
}

extend<T> A<T> {
    func foo() {
        return <!UNRESOLVED_REFERENCE!>test<!>()
    }
}

main() {
}
