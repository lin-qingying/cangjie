// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: testa.cj
// EXEC: %compiler %cmp_macro testa.cj
// EXEC: %compiler %f -o %output
// EXEC: %run %output

import define_10004.*
import std.ast.Tokens

main() {
    var a = quote(1+1) // empty lambda
    a.dump()
    return 0
}
