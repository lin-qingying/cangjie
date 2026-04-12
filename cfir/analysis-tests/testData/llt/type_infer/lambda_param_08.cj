// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// global infer triggered by generic func all

import std.collection.*

func f27<T>(f1: (T) -> Unit, f2: (T) -> Unit) {}

let v27 = f27({x => "${x}"}, {x => x + 1})

func f28<T, R>(f1: (Map<T, R>) -> Unit) where T <: Equatable<T> {}

let v28 = f28({m => m[0] = "hello world"})

main() {
    println(v27)
    println(v28)
}
