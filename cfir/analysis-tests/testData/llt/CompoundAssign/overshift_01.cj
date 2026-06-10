// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var a: Int8 = 1
    a <<= <!sema_const_eval_shift_count_overflow!>9<!>
    a >>= <!sema_const_eval_shift_count_overflow!>9<!>
    return 1
}
