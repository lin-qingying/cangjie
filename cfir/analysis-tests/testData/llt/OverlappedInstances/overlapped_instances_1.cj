// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output %run_args

func f<X, Y>(a: I<X>, b: Y) {}

interface I<X> {}

class C<X> <: I<X> {}

extend<X> C<X> <: I<D> {}

open class D {}

class E <: D {}

main() {
    f(C<E>(), false)
    0
}
