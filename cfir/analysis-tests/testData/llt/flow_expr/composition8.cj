// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

struct A{
    public static var a = (input) <!parse_expected_decl!>=><!> {input}
    static var b = (input) <!parse_expected_decl!>=><!> {input}
    static var c = (input) <!parse_expected_decl!>=><!> {input(1)}
}

main(): Int64 {
    1 |> A.a
    A.a~>A.b
    1 |> (A.a~><!parse_expected_expression!><<!>A.b)
    return (A.a ~> A.b |> A.c) - 1
}
