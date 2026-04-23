// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct S {
    static var v = 0
    const init() {
        <!EXPECT_CONST!>v = 0<!>
    }
}

main() {
    const s = S()
    S.v
}
