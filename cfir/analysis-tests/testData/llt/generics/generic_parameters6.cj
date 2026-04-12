// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface A<X, Y> where X <: A<X, X> & B<Y, Y>, Y <: A<Y, Y> & B<X, X> {}

interface B<X, Y> where X <: A<X, X> & B<Y, Y>, Y <: A<Y, Y> & B<X, X> {}

class C <: A<C, C> & B<C, C> {}

main(): Int64 {
    return 0
}
