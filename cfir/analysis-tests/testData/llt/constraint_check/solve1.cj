// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface C1 {}

open class A<T> where T <: C1 {
    public func foo(a: T) {
        return a
    }
}

class D<X> <: A<X> where X <: A<X> {}

func bar<T>() where T <: A<T> {
    var a: A<T> = D<T>()
    return a
}

class E <: A<E> & C1 {}

main() {
    var obj = bar<E>()
    return 0
}
