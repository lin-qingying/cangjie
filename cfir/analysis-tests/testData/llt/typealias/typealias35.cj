// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class GennericClassA<A, B> {}

type TypeClass<T> = GennericClassA<T, T>

type C<X> = TypeClass<Array<X>>

interface InterfaceI<X> {}

extend<Y> C<Y> <: InterfaceI<Y> {}

main() {
    var i: InterfaceI<Int64> = <!sema_mismatched_types!>C<Int64>()<!>
    0
}
