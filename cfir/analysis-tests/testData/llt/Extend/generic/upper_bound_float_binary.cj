// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C<<!sema_upper_bound_must_be_class_or_interface!>T<!>, <!sema_upper_bound_must_be_class_or_interface!>V<!>> where T <: Float32, V <: Float32 {
    func f(a: T) {
        return a <!sema_invalid_binary_expr!>+<!> a
    }
    func g(a: T) {
        return a <!sema_invalid_binary_expr!>+<!> 1.0
    }
    func g(a: T, b: V) {
        return a <!sema_invalid_binary_expr!>+<!> b
    }
    func g(a: T, b: Float32, c: V) {
        return 1.0 + a + 1.0 + b + 1.0 <!sema_invalid_binary_expr!>+<!> c + 1.0
    }
}

main() {
    var c = C<Float32, Float32>()
    if (c.f(1.0) < 1.999 || c.f(1.0) > 2.001) {
        return 1
    }
    if (c.<!sema_no_match_function_declaration_for_call!>g<!>(1.0) < 1.999 || c.g(1.0) > 2.001) {
        return 2
    }
    if (c.<!sema_no_match_function_declaration_for_call!>g<!>(1.0, 2.0) < 2.999 || c.g(1.0, 2.0) > 3.001) {
        return 3
    }
    if (c.<!sema_no_match_function_declaration_for_call!>g<!>(1.0, 2.0, 3.0) < 9.999 || c.g(1.0, 2.0, 3.0) > 10.001) {
        return 4
    }
    return 0
}
