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
    public class B {}
}

var a1 : B = 1
var a2 : A.B = 1
var a3 : A.A = 1
var a4 : C = 1
var a5 : C.D =1
var a6 : F.E = 1
var a7 : I = 1
var a8 : R = 1

/* SCAN
[33mwarning[0m: '--error-count-limit' is specified multiple times. The last one is chosen.
[33mwarning[0m: '--diagnostic-format' is specified multiple times. The last one is chosen.
error: unexpected class declaration in class body
  ==> reftype.cj:15:12:
   | 
14 | class A {
   | ~~~~~ in class
15 |     public class B {}
   |            ^^^^^ unexpected class declaration
   | 

1 error generated, 1 error printed.
*/
