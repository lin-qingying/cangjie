// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    let a: Bool = true

    if (a) {
        true <!INVALID_BINARY_OPERATOR!>+<!> 0
    } else {
        false <!INVALID_BINARY_OPERATOR!>+<!> 1
    }

    let b: Bool = if (a) {
        true <!INVALID_BINARY_OPERATOR!>+<!> 0
    } else {
        false <!INVALID_BINARY_OPERATOR!>+<!> 1
    }

    if (a) {
        true <!INVALID_BINARY_OPERATOR!>+<!> 0
    }
    let c: Bool = if (a) {
        true <!INVALID_BINARY_OPERATOR!>+<!> 0
    }
    0
}
