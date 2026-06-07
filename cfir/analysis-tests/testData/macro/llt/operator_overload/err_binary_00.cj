// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

macro package define

internal import std.ast.*

public macro SumOfEachAbs(input: Tokens) {
    let x = parseExpr(input) | return quote($<!UNRESOLVED_REFERENCE!>x<!>)
    input
}

main() {}
