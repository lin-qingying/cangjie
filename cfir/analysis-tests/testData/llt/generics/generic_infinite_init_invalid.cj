// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

// generic recursive checking for invalid type. was stackoverflow, should terminate normally

struct A<X,Y>{
    public operator func <=(b:A<Int32,(Int32,A<Int32,Int32)>):Bool {true}
}

main() {}

/* SCAN
error: unclosed delimiter: '<'
  ==> generic_infinite_init_invalid.cj:12:59:
   | 
12 |     public operator func <=(b:A<Int32,(Int32,A<Int32,Int32)>):Bool {true}
   |                                               ~           ^ expected '>' here
   |                                               |
   |                                               to match this opening '<'
   | 

1 error generated, 1 error printed.
*/
