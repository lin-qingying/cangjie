// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    var b = 1 <!INVALID_BINARY_OPERATOR!>+<!> true
    var a = b

    a **= 1
    a *= true
    a /= true
    a %= true
    a += true
    a -= true
    a <<= true
    a >>= true
    a &&= true
    a ||= true
    a &= true
    a ^= true
    a |= true

    0
}
