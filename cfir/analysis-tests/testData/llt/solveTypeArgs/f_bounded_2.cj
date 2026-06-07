// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I<X> {}

open class C <: I<C> {}

class D <: C {}

func f<<!sema_generic_param_directly_recursive!>X<!>, Y, Z>(a: X) where X <: Y, Y <: I<Y> & Z, Z <: X {}

main() {
    f(D())
    0
}
