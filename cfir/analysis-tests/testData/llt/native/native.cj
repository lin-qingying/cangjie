// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


//brief:IntNative赋值为常量计算且溢出，编译报错
main(): Int64 {
    var _: IntNative = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>-9223372036854775808 - 1<!>
    var _: IntNative = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>9223372036854775807 + 1<!>
    return 0
}
