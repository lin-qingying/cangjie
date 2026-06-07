// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func test1() {
    let newNum = if (true) {
        "let x = 1"
    } else <!chir_dce_unreachable_block_in_expression!>{
        "\"仓颉\""
    }<!>
    println(newNum is ToString)
}

func test2() {
    let newNum = if (true) {
        2
    } else <!chir_dce_unreachable_block_in_expression!>{
        0
    }<!>
    println(newNum is ToString)
}

func test3() {
    let newNum = if (true) {
        0.00
    } else <!chir_dce_unreachable_block_in_expression!>{
        0.0
    }<!>
    println(newNum is ToString)
}

func test4() {
    let newNum = if (true) {
        [true]
    } else <!chir_dce_unreachable_block_in_expression!>{
        [false]
    }<!>
    println(newNum is ToString)
}

main() {
    test1()
    test2()
    test3()
    test4()
}
