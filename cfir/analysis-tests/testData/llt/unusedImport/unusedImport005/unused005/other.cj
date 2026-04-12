macro package a

public import std.deriving.Derive
public import std.collection.ArrayList
import std.ast.*

public macro ModifyFunc(input: Tokens): Tokens {
    let funcDecl = FuncDecl(input)
    return quote(
        func $(funcDecl.identifier)() {
            println("start")
            $(funcDecl.block.nodes)
            println("end")
        })
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
