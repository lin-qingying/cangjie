// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class MyException <: Exception {}

main(): Int64 {
    throw Exception() // ok
    throw MyException() // ok
    <!THROW_EXPR_WITH_WRONG_TYPE!>throw<!> 1
    return 0
}
