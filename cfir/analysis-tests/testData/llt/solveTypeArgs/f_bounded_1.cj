// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I<X> {}

open class C <: I<C> {}

class D <: C {}

func f<<!sema_upper_bound_must_be_class_or_interface!>Y<!>, Z>(a: Y) where Y <: Z, Z <: I<Y> {}

main() {
    f(D()) // see below
    0
}
