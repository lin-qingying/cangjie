// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args | compare %f

interface I {}

struct Foo <: I {
    Foo() {
        println("Foo")
    }
}

main() {
    println(1 is Int64)
    println(1 is Any)
    println(Foo() is Foo)
    println(Foo() is I)
    println(None<Bool> is Option<Bool>)
}

/* SCAN
true
true
Foo
true
Foo
true
true
*/