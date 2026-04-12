// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Type<T, K> {
    public init(a: T, b: K) {}
}

type A1<T> = Type<Rune, T>

type B1<X> = A1<X>

type C1<x> = B1<Int64>

type A2<T, K> = Type<Rune, T>

type B2<X> = A2<Int64, X>

type C2 = B2<Rune>

main(): Int64 {
    C1<Rune>(r'a', 1)
    C2(r'a', 1)
    return 0
}
