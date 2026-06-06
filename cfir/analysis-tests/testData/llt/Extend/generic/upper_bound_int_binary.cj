// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class C<<!sema_upper_bound_must_be_class_or_interface!>T<!>, <!sema_upper_bound_must_be_class_or_interface!>V<!>> where T <: Int8, V <: Int8 {
    func f(a: T) {
        return a <!sema_invalid_binary_expr!>+<!> a
    }
    func g(a: T) {
        return a <!sema_invalid_binary_expr!>+<!> 1
    }
    func g(a: T, b: V) {
        return a <!sema_invalid_binary_expr!>+<!> b
    }
    func g(a: T, b: Int8, c: V) {
        return 1 + a + 1 + b + 1 <!sema_invalid_binary_expr!>+<!> c + 1
    }
}

main() {
    var c = C<Int8, Int8>()
    if (c.f(1) != 2) {
        return 1
    }
    if (c.<!sema_no_match_function_declaration_for_call!>g<!>(1) != 2) {
        return 2
    }
    if (c.<!sema_no_match_function_declaration_for_call!>g<!>(1, 2) != 3) {
        return 3
    }
    if (c.<!sema_no_match_function_declaration_for_call!>g<!>(1, 2, 3) != 10) {
        return 4
    }
    return 0
}
