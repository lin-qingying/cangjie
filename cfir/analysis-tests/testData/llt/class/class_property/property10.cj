// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    return 0
}

open class Boo {
    public mut override prop a6: Int64 {
    }
}

class Coo <: Boo {}

/* SCAN
error: property must have accessors
  ==> property10.cj:14:5:
   | 
14 |       public mut override prop a6: Int64 {
   |  _____^
15 | |     }
   | |_____^
   | 

error: 'override' property 'a6' does not have an overridden property in its supertype
  ==> property10.cj:14:5:
   | 
14 |     public mut override prop a6: Int64 {
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
