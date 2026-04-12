// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    return 0
}

abstract class Boo {
    private mut prop l: Int32
    public mut static prop m: Int32
}

/* SCAN
error: the visibility of an 'abstract' property must be 'public' or 'protected'
  ==> property27.cj:14:22:
   | 
14 |     private mut prop l: Int32
   |                      ^ 
   | 

error: property 'm' can not be abstract
  ==> property27.cj:15:5:
   | 
15 |     public mut static prop m: Int32
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
