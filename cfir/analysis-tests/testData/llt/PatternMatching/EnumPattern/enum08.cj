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
    var d: Int64 = match (time) {
        case Year(a) => a
        case Month(a, a) => a + a
        case _ => -1
    }
    if (d == 6) {
        return 0
    }
    return 1
}

/* SCAN
error: mismatched types
  ==> enum08.cj:19:25:
   | 
19 |         case Year(a) => a
   |                         ^ expected 'Int64', found 'Int32'
   | 

error: redefinition of declaration 'a'
  ==> enum08.cj:20:23:
   | 
20 |         case Month(a, a) => a + a
   |                       ^ 
   | 
note: 'a' is previously declared here
  ==> enum08.cj:20:20:
   | 
20 |         case Month(a, a) => a + a
   |                    ^ 
   | 

error: mismatched types
  ==> enum08.cj:20:29:
   | 
20 |         case Month(a, a) => a + a
   |                             ^^^^^ expected 'Int64', found 'Int32'
   | 

3 errors generated, 3 errors printed.
*/
