// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface D<T> {}

class C <: D<Int32> {}

class A<T> {
    public func test(a: A<T>): C {
        return C()
    }
}

main(): Int64 {
    var a: A<D<Int32>> = A<D<Int32>>()
    var b: A<C> = A<C>()
    var c: A<D<Int8>> = A<D<Int8>>()
    var d: A<Int8> = A<Int8>()
    return 0
}
