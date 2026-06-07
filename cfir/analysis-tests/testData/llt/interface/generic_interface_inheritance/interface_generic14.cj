// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C {}

class D {}

class E {}

interface I<T> {
    func foo(): T
}

interface II<T> <: I<C> & I<D> & I<E> {
    func <!OVERRIDING_RETURN_TYPE_MISMATCH!>foo<!>(): C
}

main() {
    return 0
}
