// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func mango() {
    "🥭"
}

let kiwi = "🥝"
let lemon = "🍋"

main() {
    const x = <!EXPECT_CONST!>mango<!>() + <!EXPECT_CONST!>kiwi<!>
    const y = <!EXPECT_CONST!>mango<!>() + <!EXPECT_CONST!>lemon<!>
    const z = <!EXPECT_CONST!>kiwi<!> * 3
}
