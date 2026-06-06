// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main(): Int64 {
    var a: Int8 = 1
    a <<= <!CONST_EVAL_SHIFT_COUNT_OVERFLOW!>9<!>
    a >>= <!CONST_EVAL_SHIFT_COUNT_OVERFLOW!>9<!>
    return 1
}
