// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func run(): Int64 {
    var _: Int64 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 ** 62 + -2 ** 62<!> - 1
    var _: Int64 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 ** 63<!>

    var _ = UInt64(<!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 ** 63<!> + <!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 ** 63<!> - 1)
    var _ = UInt64(<!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 ** 64<!>)
    return 0
}

main(): Int64 {
    return run();
}
