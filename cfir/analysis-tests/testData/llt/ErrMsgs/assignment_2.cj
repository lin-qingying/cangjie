// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// Should only report type mismatch at line 10

class A {
    operator func -() {
        1
    }
}

main() {
    let c = 1
    let a: Rune = <!TYPE_MISMATCH!>-A()<!>
    let b = <!INVALID_UNARY_EXPR!>- r'c'<!>
}
