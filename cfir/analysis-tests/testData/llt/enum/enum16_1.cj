// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A2 {}

enum A {
    A1(Int32) | A2
}

enum AA {
    A1 | A3 | A4 | A5
}

func A1() {}

let A3 = 1

main(): Int64 {
    var a = A1
    var A2 = A2
    var b = match (A2) {
        case A1(x) => 0
        case _ => -1
    }
    let A4 = 1
    var a1 = AA.A1
    var c = match (a1) {
        case A3 => 0
        case _ => -1
    }
    var aa = AA.A1
    var d = match (aa) {
        case A4 => 0
        case _ => -1
    }
    let A5 = AA.A1
    var e = match (aa) {
        case A5 => 0
        case _ => -1
    }

    return 0
}

/* SCAN
error: enum pattern is not matched
  ==> enum16_1.cj:27:14:
   | 
27 |         case A1(x) => 0
   |              ^ 
   | 

error: expected member name or constructor call after 'A2' type name
  ==> enum16_1.cj:25:14:
   | 
25 |     var A2 = A2
   |              ^ 
   | 
note: found candidate
 ==> enum16_1.cj:9:1:
  | 
9 | class A2 {}
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
