// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var a: Int8 = 1
    var b: Int16 = 1
    var c: Int32 = 1
    var d: Int64 = 1
    var e: Int64 = 1
    var r1 = a <!INVALID_BINARY_OPERATOR!>&<!> b
    var r2 = a <!INVALID_BINARY_OPERATOR!>^<!> c
    var r3 = a <!INVALID_BINARY_OPERATOR!>|<!> e
    var r4 = a <!INVALID_BINARY_OPERATOR!>&<!> b ^ c << d | e
    var r5 = a <!INVALID_BINARY_OPERATOR!>|<!> e << c ^ d | b
    return 0
}
