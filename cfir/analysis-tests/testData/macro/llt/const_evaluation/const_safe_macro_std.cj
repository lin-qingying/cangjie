// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package std.example

internal import std.ast.*

public macro <!parse_macro_define_conflicted_with_builtin!>ConstSafe<!>(inpt: Tokens): Tokens { inpt }
