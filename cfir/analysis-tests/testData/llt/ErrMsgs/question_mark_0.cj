// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C {}

main() {
    let err = 1 <!sema_invalid_binary_expr!>+<!> false
    let optLam: Option<() -> Unit> = {=>}
    let a = optLam?<!sema_wrong_number_of_arguments!>(1)<!>
    a + 1
    (err)?(1)

    let optTup = Some((1, true))
    let b = optTup?[<!sema_builtin_index_in_bound!>2<!>]
    b - 2
    (err)?[1]

    let optC = Some(C())
    let c = optC?.<!sema_not_member_of!>x<!>
    c * 3
    (err)?.x

    0
}
