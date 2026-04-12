// EXEC: %compiler %f -Woff unused

import std.collection.*

func foo(a: T1) {}

type T1 = T2
type T2 = T3 
type T3 = T4
type T4 = Int64

type K1 = K2
type K2 = K3
type K3 = K4
type K4 = UInt64

type M1<T, V> = M2<T, V>
type M2<T, V> = M3<T, V>
type M3<T, V> = HashMap<T, V>

func bar(a: M1<T2, K1>) {}

main() {}


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
