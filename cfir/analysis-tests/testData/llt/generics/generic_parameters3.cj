// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A<T> {}

class B<T, V> <: <!GENERIC_ARGUMENT_NO_MATCH!>A<T, V><!> {}

class C<T, V> <: <!GENERIC_ARGUMENT_NO_MATCH!>A<V, T><!> {}

class D<T1, T2, T3> <: <!GENERIC_ARGUMENT_NO_MATCH!>A<T1, T2, T3><!> {}

main() {
    return 0
}
