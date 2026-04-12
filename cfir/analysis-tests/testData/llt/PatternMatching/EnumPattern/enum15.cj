// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

enum TokenID {
    ADD
    | MINUS
    | MUL
    | DIVIDE
}
main():Int64 {
    var isbinary : Bool = match (id) {
        case .ADD => true 
        case _ => false
    }
    return 0
}

/* SCAN
error: expected pattern, found '.'
  ==> enum15.cj:17:14:
   | 
17 |         case .ADD => true 
   |              ^ expected pattern here
   | 

1 error generated, 1 error printed.
*/
