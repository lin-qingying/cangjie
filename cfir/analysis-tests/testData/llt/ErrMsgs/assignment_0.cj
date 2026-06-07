// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var x: <!sema_undeclared_type_name!>C<!><<!sema_undeclared_type_name!>T<!>> = <!sema_no_match_operator_function_call!>1<!>(true)
    var (a1, a2): (<!sema_undeclared_type_name!>C<!><<!sema_undeclared_type_name!>T<!>1>, <!sema_undeclared_type_name!>C<!><<!sema_undeclared_type_name!>T<!>2>) = (false, false)

    x = 1
    a1 = (false, false)
    a2 = false
    0
}
