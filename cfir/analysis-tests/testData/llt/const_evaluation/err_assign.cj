// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

var g = 0

struct S {
    var x: Int64
    const init(x: Int64) {
        this.x = x
        <!EXPECT_CONST!>g = 1<!>
        <!EXPECT_CONST!>g++<!>
    }
}

main() {
    const s = S(-1)
    s.x
}
