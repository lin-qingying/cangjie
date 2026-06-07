// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    var err = 1 <!sema_invalid_binary_expr!>+<!> false

    match (err) {
        case x => 1 + false
        case y => 1 + true
        case z => err
        case phi => phi
    }

    0
}

// Errors:

// No errors:
