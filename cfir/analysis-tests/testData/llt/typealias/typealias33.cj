// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-0: %compiler %cmp_opt %f -o %output 2>&1

class GennericClassA<T> {}

type TypeClass<Y> = GennericClassA<Y>

type TypeB<X> = TypeClass<X>

main() {
    var f1: TypeB<Int64> = TypeB()
    var g1: TypeClass<Int64> = TypeB()
    var h1: GennericClassA<Int64> = TypeB()
    var i1: TypeB<Int64> = TypeClass()
    var j1: TypeClass<Int64> = TypeClass()
    var k1: GennericClassA<Int64> = TypeClass()
    var l1: TypeB<Int64> = GennericClassA()
    var m1: TypeClass<Int64> = GennericClassA()
    var n1: GennericClassA<Int64> = GennericClassA()

    var f2 = TypeB<Int64>()
    var g2 = TypeB<Int64>()
    var h2 = TypeB<Int64>()
    var i2 = TypeClass<Int64>()
    var j2 = TypeClass<Int64>()
    var k2 = TypeClass<Int64>()
    var l2 = GennericClassA<Int64>()
    var m2 = GennericClassA<Int64>()
    var n2 = GennericClassA<Int64>()

    var f3: TypeB<Int64> = TypeB<Int64>()
    var g3: TypeClass<Int64> = TypeB<Int64>()
    var h3: GennericClassA<Int64> = TypeB<Int64>()
    var i3: TypeB<Int64> = TypeClass<Int64>()
    var j3: TypeClass<Int64> = TypeClass<Int64>()
    var k3: GennericClassA<Int64> = TypeClass<Int64>()
    var l3: TypeB<Int64> = GennericClassA<Int64>()
    var m3: TypeClass<Int64> = GennericClassA<Int64>()
    var n3: GennericClassA<Int64> = GennericClassA<Int64>()
    0
}
