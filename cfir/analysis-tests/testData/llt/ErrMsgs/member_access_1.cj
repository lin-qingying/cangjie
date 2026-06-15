// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false

    <!GENERIC_ARGUMENT_NO_MATCH!>err<Bool><!>
    <!GENERIC_ARGUMENT_NO_MATCH!>err<Bool><!>.e2
    <!GENERIC_ARGUMENT_NO_MATCH!>err<Bool><!>.e2(e3)
    <!GENERIC_ARGUMENT_NO_MATCH!>err<Bool><!>.e2<Bool>
    <!GENERIC_ARGUMENT_NO_MATCH!>err<Bool><!>.e2<Bool>(e3)

    err.e2
    err.e2(e3)
    err.e2<Bool>
    err.e2<Bool>(e3)

    0
}

// Errors:

// No errors:
