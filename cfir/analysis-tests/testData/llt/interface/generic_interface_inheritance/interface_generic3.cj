// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I<T> {
    func foo(a: T): Int32
}

interface II<T> <: I<T> & I<Int32> & <!SUPER_TYPES_DUPLICATE!>I<Int32><!> {
    func foo(a: Int32): Int32
    func foo(a: T): Int32
}

main() {
    return 0
}
