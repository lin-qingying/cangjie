// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let err1 = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    "a${err1}c"
    let b: String = "a${err1}c"

    let err2 = "a${err1}c"

    err2 + 1
    err2 * false
    err2(true)
    0
}
