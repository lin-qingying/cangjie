// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

enum A {
    A1(Int32) | A2
}

enum AA {
    A1
}

func A1() {}

main(): Int64 {
    var a: AA = A1
    var A2 = A2 // left A2 is enum constructor.
    var b = match (A2) {
        case A2 => 0
        case _ => -1
    }

    var c = match (a) {
        case A1 => 0
        case _ => -1
    }

    return b
}

/* SCAN
error: mismatched types
  ==> enum16.cj:20:17:
   | 
20 |     var a: AA = A1
   |                 ^^ expected 'Enum-AA', found '() -> Unit'
   | 

warning: unreachable pattern
  ==> enum16.cj:29:14:
   | 
29 |         case _ => -1
   |              ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

1 error generated, 1 error printed.
1 warning generated, 1 warning printed.
*/
