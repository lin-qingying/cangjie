// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %cmp_opt %typecheck %cmp_opt %f 2>&1 | compare %f

interface I {}
struct R{}
struct re {
   public let a : Int32
}
class A {
    public <!parse_unexpected_declaration_in_scope!>class<!> B {}
}

var a1 : B = 1
var a2 : A.B = 1
var a3 : A.A = 1
var a4 : C = 1
var a5 : C.D =1
var a6 : F.E = 1
var a7 : I = 1
var a8 : R = 1
