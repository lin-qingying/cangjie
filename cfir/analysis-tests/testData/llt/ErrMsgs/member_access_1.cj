// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let err = 1 <!sema_invalid_binary_expr!>+<!> false

    <!sema_generic_argument_no_match!>err<Bool><!>
    <!sema_generic_argument_no_match!>err<Bool><!>.e2
    <!sema_generic_argument_no_match!>err<Bool><!>.e2(e3)
    <!sema_generic_argument_no_match!>err<Bool><!>.e2<Bool>
    <!sema_generic_argument_no_match!>err<Bool><!>.e2<Bool>(e3)

    err.e2
    err.e2(e3)
    err.e2<Bool>
    err.e2<Bool>(e3)

    0
}

// Errors:

// No errors:
