// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %n.%suffix %run_args | compare %f

func test1() {
    let newNum = if (true) {
        "let x = 1"
    } else {
        "\"仓颉\""
    }
    println(newNum is ToString)
}

func test2() {
    let newNum = if (true) {
        2
    } else {
        0
    }
    println(newNum is ToString)
}

func test3() {
    let newNum = if (true) {
        0.00
    } else {
        0.0
    }
    println(newNum is ToString)
}

func test4() {
    let newNum = if (true) {
        [true]
    } else {
        [false]
    }
    println(newNum is ToString)
}

main() {
    test1()
    test2()
    test3()
    test4()
}

// ASSERT: scan-4 true
