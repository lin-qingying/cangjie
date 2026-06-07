// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func f<X, Y>(a: X, b: Y) where X <: I<X> & I<C>, Y <: I<Y> & I<C> {}

interface I<X> {}

open class C <: I<C> {}

open class D <: C & I<D> {}

open class E <: D & I<E> {}

main() {
    f(E(), E())
    0
}
