// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func test1() {
    let newNum = if (false) <!chir_dce_unreachable_block_in_expression!>{
        "let x = 1"
    }<!> else {
        "\"仓颉\""
    }
    println(newNum)
    println(newNum is String)
}

func test2() {
    let newNum = if (false) <!chir_dce_unreachable_block_in_expression!>{
        2
    }<!> else {
        0
    }
    println(newNum)
    println(newNum is Int64)
}

func test3() {
    let newNum = if (true) {
        let <!chir_dce_unused_variable!>x<!> = 2
    } else <!chir_dce_unreachable_block_in_expression!>{
        let y = 0.0
    }<!>
    println(newNum)
    println(newNum is Unit)
}

func test4() {
    let newNum = if (false) <!chir_dce_unreachable_block_in_expression!>{
        [true]
    }<!> else {
        [false]
    }
    println(newNum)
    println(newNum is Array<Bool>)
}

main() {
    test1()
    test2()
    test3()
    test4()
}
