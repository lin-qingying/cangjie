// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class GennericClassA<T, V> {}

type TypeClass1<T, V> = GennericClassA<T, V>

type TypeClass2<T> = GennericClassA<T, T>

type A = TypeClass1<Int64, Int8>

type B<X> = TypeClass2<X>

main() {
    var a1: <!GENERIC_ARGUMENT_NO_MATCH!>A<Int32><!>
    var a2: A = <!GENERIC_ARGUMENT_NO_MATCH!>A<Int32>()<!>
    var b1: <!GENERIC_ARGUMENT_NO_MATCH!>B<Int64, Int32><!>
    var b2 = <!GENERIC_ARGUMENT_NO_MATCH!>B<Int64, Int32>()<!>
    0
}
