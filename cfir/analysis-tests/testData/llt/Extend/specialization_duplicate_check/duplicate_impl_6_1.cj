// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<T> {}

class A<K> {}

class B<R> {}

extend Array<A<Int64>> <: I<Int64> {}

extend<T> Array<B<T>> <: I<T> {}

main() {}
