// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -warnings

import std.convert.Formattable
import std.math.abs

main() {
    println(<!UNRESOLVED_REFERENCE!>pow<!>(1.2, 2.3))
}
