// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-0: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
// EXEC: %run %run_opt %output %run_args

class GennericClassA<T, V> {}

type TypeClass1<T, V> = GennericClassA<T, V>

type TypeClass2<T> = GennericClassA<T, T>

type A = TypeClass1<Int64, Int8>

type B<X> = TypeClass2<X>

type C<X> = TypeClass2<Array<X>>

interface InterfaceI<X> {}

extend<Y> C<Y> <: InterfaceI<Y> {}

main() {
    var a: A = A()
    var b: B<Int64> = B<Int64>()
    var c: C<Int64> = GennericClassA<Array<Int64>, Array<Int64>>()
    var i: InterfaceI<Int64> = C<Int64>()
    0
}
