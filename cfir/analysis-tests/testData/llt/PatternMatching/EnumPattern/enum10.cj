// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
    | Hour(Int32, Int32, Int32, Int32)
}

main(): Int64 {
    var time = Day(1, 2, 3)
    var d = match (time) {
        case Year(y) => y
        case Year(z) | Month(y, _) => z
        case _ => -1
    }
    return 0
}

/* SCAN
error: cannot introduce variables in patterns connected by '|'
  ==> enum10.cj:20:19:
   | 
20 |         case Year(z) | Month(y, _) => z
   |                   ^          ~ 
   | 

error: undeclared identifier 'z'
  ==> enum10.cj:20:39:
   | 
20 |         case Year(z) | Month(y, _) => z
   |                                       ^ 
   | 

2 errors generated, 2 errors printed.
*/
