// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    if (String.n != 1) {
        return 1
    }
    return 0
}

extend String {
    public mut static prop n: Int32 {
    }
}

/* SCAN
error: property must have accessors
  ==> property28.cj:17:5:
   | 
17 |       public mut static prop n: Int32 {
   |  _____^
18 | |     }
   | |_____^
   | 

error: there cannot have mutable property in immutable type
  ==> property28.cj:17:12:
   | 
17 |     public mut static prop n: Int32 {
   |            ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
