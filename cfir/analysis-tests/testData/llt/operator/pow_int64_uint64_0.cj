// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let a1 = 1 <!INVALID_BINARY_OPERATOR!>**<!> -1
    let a2: UInt64 = 1 <!INVALID_BINARY_OPERATOR!>**<!> -1
    let b1 = -1 <!INVALID_BINARY_OPERATOR!>**<!> -1
    let b2: UInt64 = -1 <!INVALID_BINARY_OPERATOR!>**<!> -1
    let c = Int8(0) <!INVALID_BINARY_OPERATOR!>**<!> 0
    let d = -1 <!INVALID_BINARY_OPERATOR!>**<!> r'c'
    // no need to test more primitive types
    let e1 = A() <!INVALID_BINARY_OPERATOR!>**<!> 0
    let e2 = 0 <!INVALID_BINARY_OPERATOR!>**<!> A()
    0
}

class A {}
