// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

import std.ast.*
import std.deriving.*

@Annotation
public class APILevel {
    public const init() {}
}

func foo() {
    let tokens = quote(
    public enum Battery {
        @APILevel
        NONE
        | ENABLE
    }
    )
    let decl = parseDecl(tokens)
    let e = (decl as EnumDecl).getOrThrow()
    println(e.toTokens().toString())
}

main() {
    foo()
    0
}
