// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct A {
    const func x() {
        0
    }
    const init() {}
    func f() {
        const t = <!EXPECT_CONST!>x<!>
        t
    }
}

main() {}
