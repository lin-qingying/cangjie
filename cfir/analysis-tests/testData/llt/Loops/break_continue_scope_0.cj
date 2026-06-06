// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %n.%suffix %run_args | compare %f

main() {
    while (true) {
        println("outer")
        do {
            <!chir_dce_unreachable_expression_hint!>while (continue) {
                println("here")
            }<!>
            println("inner")
        } while (break)
    }
    0
}
