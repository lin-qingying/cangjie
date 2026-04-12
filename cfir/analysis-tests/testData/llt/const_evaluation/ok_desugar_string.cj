// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

const func mango() {
    "🥭"
}

const kiwi = "🥝"
const lemon = "🍋"

const func f() {
    mango() + kiwi == "🥭🥝" && mango() + lemon == "🥭🍋" && kiwi * 3 == "🥝🥝🥝" && "a" < "b" && "a" <= "b" && "b" > "a" &&
        "b" >= "a" && mango() != kiwi
}

main() {
    if (f()) {
        0
    } else {
        1
    }
}
