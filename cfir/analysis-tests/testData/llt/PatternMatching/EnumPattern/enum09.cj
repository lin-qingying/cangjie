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
        case Year(y) | Month(y, m) => y
        case _ => -1
    }
    return 0
}

/* SCAN
error: cannot introduce variables in patterns connected by '|'
  ==> enum09.cj:19:19:
   | 
19 |         case Year(y) | Month(y, m) => y
   |                   ^          ~  ~ 
   | 

error: undeclared identifier 'y'
  ==> enum09.cj:19:39:
   | 
19 |         case Year(y) | Month(y, m) => y
   |                                       ^ 
   | 

2 errors generated, 2 errors printed.
*/
