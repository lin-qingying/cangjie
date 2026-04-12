// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %n.%suffix %run_args | compare %f
// ASSERT: regex 2

interface I5 {}

interface I2 {}

interface I6 <: I2 & I5 {}

interface I4 <: I5 & I6 {}

interface I3 <: I5 & I4 {}

interface I1 <: I4 & I2 {}

struct A <: I1 & I2 {}

struct B <: I3 & I4 {}

main() {
    let x = [A(), B()]
    println(x.size)
}
