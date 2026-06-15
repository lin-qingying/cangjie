// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {}

extend A {
    func f() {
        0
    }
}

const func f() {
    let a = <!EXPECT_CONST!>A<!>()
    <!EXPECT_CONST!>a.f<!>()
}

main() {
    f()
}
