// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C {}

main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    let optLam: Option<() -> Unit> = {=>}
    let a = optLam?<!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>
    a + 1
    (err)?(1)

    let optTup = Some((1, true))
    let b = optTup?[<!BUILTIN_INDEX_IN_BOUND!>2<!>]
    b - 2
    (err)?[1]

    let optC = Some(C())
    let c = optC?.<!NOT_MEMBER_OF!>x<!>
    c * 3
    (err)?.x

    0
}
