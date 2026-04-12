// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main(): Int64 {
    var a: Int32 = 1

    func foo() {
        return a
    }

    var ret = if (true) {
        foo
    } else {
        foo
    }

    if (ret() != 1) {
        return 1
    }

    return 0
}

/* SCAN
error: function capturing mutable variables needs to be called directly
  ==> capture7.cj:17:9:
   | 
17 |         foo
   |         ^^^ 
   | 
note: 'a' is mutable
  ==> capture7.cj:13:16:
   | 
13 |         return a
   |                ^ 
   | 

error: function capturing mutable variables needs to be called directly
  ==> capture7.cj:19:9:
   | 
19 |         foo
   |         ^^^ 
   | 
note: 'a' is mutable
  ==> capture7.cj:13:16:
   | 
13 |         return a
   |                ^ 
   | 

2 errors generated, 2 errors printed.
*/
