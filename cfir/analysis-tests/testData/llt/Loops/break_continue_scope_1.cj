// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    while (true) {
        println("outer")
        do {
            while ((continue, <!chir_dce_unreachable_expression_hint!>println("baka")<!>)[0]) {
                println("here")
            }
            println("inner")
        } while ((println("aho"), return break, "aho")[1])
    }
    0
}
