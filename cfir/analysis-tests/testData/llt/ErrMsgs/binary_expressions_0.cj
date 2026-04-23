// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    var a = 1 <!INVALID_BINARY_OPERATOR!>+<!> true
    1 <!INVALID_BINARY_OPERATOR!>+<!> true + a

    1 + 2 + a
    a - 2 - 3
    a ** 1
    a * true
    a / true
    a % true
    a + true
    a - true
    a << true
    a >> true
    a && true
    a || true
    a & true
    a ^ true
    a | true

    0
}
