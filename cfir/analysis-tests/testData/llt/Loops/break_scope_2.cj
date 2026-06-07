// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    for (<!chir_dce_unused_variable!>i<!> in 1..5) {
        println("outer")
        <!chir_dce_unreachable_expression_hint!>while (break) {
            println("inner")
        }<!>
    }
    0
}
