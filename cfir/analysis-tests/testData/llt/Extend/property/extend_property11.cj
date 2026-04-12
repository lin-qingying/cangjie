// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main() : Int64 {
    var a = ""
    if( String.n != 1) {
        return 1
    }
    a.m= 10
    if( a.m != 2) {
        return 2
    }
    return 0
}

extend String {
    public mut prop m: Int32 
    public static prop n: Int32 
}

/* SCAN
error: expected '{', found keyword 'public'
  ==> extend_property11.cj:23:5:
   | 
23 |     public static prop n: Int32 
   |     ^^^^^^ expected '{' here
   | 
   # note: property declaration must have body

error: expected '{', found '}'
  ==> extend_property11.cj:24:1:
   | 
24 | }
   | ^ expected '{' here
   | 
   # note: property declaration must have body

2 errors generated, 2 errors printed.
*/
