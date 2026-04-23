// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let a = 1 <!INVALID_BINARY_OPERATOR!>+<!> true

    while (a) {
        let b = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    }

    do {
        let b = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    } while (a)

    for (x in [1] where a) {
        let b = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    }

    for (a in a) {}

    0
}
