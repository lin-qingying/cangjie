// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var a: Int8 = 1
    var b: Int8 = 2
    var c: Int8 = 4
    var d: Int8 = 9
    a <<= <!sema_literal_numeric_overflow!>128<!>
    a >>= <!sema_literal_numeric_overflow!>128<!>
    a <<= <!sema_const_eval_negative_shift_count!>-1<!>
    a >>= <!sema_const_eval_negative_shift_count!>-1<!>
    a += <!sema_literal_numeric_overflow!>128<!>
    b -= <!sema_literal_numeric_overflow!>128<!>
    c *= <!sema_literal_numeric_overflow!>128<!>
    d /= <!sema_literal_numeric_overflow!>128<!>
    return 1
}
