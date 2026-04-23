// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {}

class B<T> {}

class C<T> {}

extend<T> A<C<T>> {
    func test() {
        22
    }
}

main() {
    let a1 = A<B<Int64>>()
    a1.<!NOT_MEMBER_OF!>test<!>()
    return 0
}
