// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    let x = false
    <!TYPE_MISMATCH, CANNOT_ASSIGN_TO_IMMUTABLE!>x++<!> + 1
    <!TYPE_MISMATCH, CANNOT_ASSIGN_TO_IMMUTABLE!>x--<!> * 2

    var y = err
    y++
    y--

    let z = err
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>z++<!>
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>z--<!>

    0
}
