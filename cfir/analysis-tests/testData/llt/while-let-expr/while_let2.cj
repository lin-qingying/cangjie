// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

var sum = 0

main() {
    println("test if-let expression")

    // ConstPattern
    // case 10 | 20
    let x1 = 42
    let x11 = 43
    while (let 42 <- x1) {
        println("ConstPattern works")
        sum = sum + 1 // Ok sum 1
        break
    }

    while (let 42 <- x11) {
        println("ConstPattern works")
        break
    }

    // WildCardPattern
    // case _
    let x2 = Some<Int32>(42)
    while (let _ <- x2) {
        println("WildCardPattern works")
        sum = sum + 1 // Ok sum 2
        break
    }

    // BindingPattern
    let x3 = 1
    while (let p <- x3) {
        println("BindingPattern works")
        sum = sum + p // Ok sum 3
        break
    }

    // TupelPattern
    // case ("Bob", 90) =>
    let x4 = ("Bob", 90)
    while (let ("Bob", 80) <- x4) {
        println("TupelPattern works")
        sum = sum - 1
        break
    }
    sum = sum + 1 // Ok sum 4

    while (let ("Bob", 90) <- x4) {
        println("TupelPattern works")
        sum = sum + 1 // Ok sum 5
        break
    }

    // EnumPattern
    // case TimeUnit.Year(y) =>
    // case Year(y) | Month(y, m) =>
    let x5: Option<Int64> = Option<Int64>.Some(100)
    let x55: Option<Int64> = Option<Int64>.None
    while (let Some(v) <- x5) {
        println("x55 has value v")
        println("v is ${v}")
        sum = sum + 1 // Ok sum 6
        break
    }

    while (let Some(v) <- x55) {
        println("x55 has value v")
        println("v is ${v}")
        sum = sum - 1
        break
    }
    sum = sum + 1 // Ok sum 7

    println("sum is ${sum}")
    if (sum != 7) {
        return 1
    }

    0
}
