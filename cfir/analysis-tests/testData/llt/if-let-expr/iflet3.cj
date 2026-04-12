// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

var foo = 0
var bar = 0

main() {
    let x2 = Some<Int32>(42)
    let x3 = Option<Int32>.None
    let x4 = (1, 2, "hello")
    if (let (1, 3, "hello") <- x4) {
        println("TuplePat Works")
    } else if (let (1, 2, "hello") <- x4) {
        println("Nested Matched #1")
        foo = 1
    } else if (let _ <- x3) {
        println("Nested Matched #2")
    } else {
        println("final else")
    }

    if (let (2, 2, "hello") <- x4) {
        println("Matched #1")
    } else if (let Some(v) <- x3) {
        bar = 2
    } else if (let _ <- x3) {
        println("Nested Matched #2")
        bar = 1
    } else {
        println("final else")
    }

    if (foo != 1 || bar != 1) {
        return 1
    }

    0
}
