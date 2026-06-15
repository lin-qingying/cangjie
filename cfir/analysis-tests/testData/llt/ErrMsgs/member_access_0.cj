// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    let a = 1 <!INVALID_BINARY_OPERATOR!>+<!> true

    (false <!INVALID_BINARY_OPERATOR!>+<!> 2).f()
    a.f()
    a.x

    0
}

// No errors:

// Errors:
