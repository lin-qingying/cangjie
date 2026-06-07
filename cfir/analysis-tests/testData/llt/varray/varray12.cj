// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
// DIAGNOSTICS: -warnings


func foo() {
    var i = 0
    while (i < 5) {
        i++;
        let arr1: VArray<Nothing, $3> = [continue, <!chir_dce_unreachable_expression_hint!>break<!>, return 0]
        arr1[0]
    }
    if (i != 5) {
        return 1
    }
    return 0
}

func foo1() {
    var i = 0
    while (i < 5) {
        i++;
        let arr1: VArray<Int64, $3> = [<!chir_dce_unused_expression!>1<!>, break, <!chir_dce_unreachable_expression_hint!>return 0<!>]
        arr1[0]
    }
    if (i != 1) <!chir_dce_unreachable_block_in_expression!>{
        return 1
    }<!>
    return 0
}

main() {
    return foo() + foo1()
}
