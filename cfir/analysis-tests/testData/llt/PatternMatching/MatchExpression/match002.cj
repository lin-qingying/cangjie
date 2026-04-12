// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func test(): Int64 {
    var i: Int32 = 0
    var res = match (i++) {
        case 0 => res = 1
        case 1 => res = 0
        case _ => res = -1
    }
    if (res == 0) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}

/* SCAN
error: cannot convert an integer literal to type 'Unit'
  ==> match002.cj:12:14:
   | 
12 |         case 0 => res = 1
   |              ^ 
   | 

error: undeclared identifier 'res'
  ==> match002.cj:12:19:
   | 
12 |         case 0 => res = 1
   |                   ^ 
   | 

error: cannot convert an integer literal to type 'Unit'
  ==> match002.cj:13:14:
   | 
13 |         case 1 => res = 0
   |              ^ 
   | 

error: undeclared identifier 'res'
  ==> match002.cj:13:19:
   | 
13 |         case 1 => res = 0
   |                   ^ 
   | 

error: undeclared identifier 'res'
  ==> match002.cj:14:19:
   | 
14 |         case _ => res = -1
   |                   ^ 
   | 

5 errors generated, 5 errors printed.
*/
