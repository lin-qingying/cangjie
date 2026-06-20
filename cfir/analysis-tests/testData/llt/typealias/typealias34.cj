// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class GennericClassA<T, V> {}

type TypeClass1<T, V> = GennericClassA<T, V>

type TypeClass2<T> = GennericClassA<T, T>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype TypeClass3<X, Y> = TypeClass2<X>

main() {
    var a1: <!GENERIC_ARGUMENT_NO_MATCH!>TypeClass3<Int32><!>
    var a2 = <!GENERIC_ARGUMENT_NO_MATCH!>TypeClass3<Int32>()<!>
    0
}
