// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %compile_lib_opt %n.cj 2>&1 | compare %f

main(): Int64 {
    let x: <!sema_undeclared_type_name!>C<!><<!sema_undeclared_type_name!>T<!>> = <!sema_no_match_operator_function_call!>1<!>(true)
    let y = <!sema_no_match_operator_function_call!>1<!>(true)
    let z: Bool = <!sema_cannot_convert_literal!>1<!>

    let (a1, a2): (<!sema_undeclared_type_name!>C<!><<!sema_undeclared_type_name!>T<!>1>, <!sema_undeclared_type_name!>C<!><<!sema_undeclared_type_name!>T<!>2>) = (false, false)
    let <!TUPLE_PATTERN_NOT_MATCH!>(<!>b1, b2) = false
    let (c1, c2): (Int64, Bool) = (<!sema_cannot_convert_literal!>t<!>rue, false)

    0
}
