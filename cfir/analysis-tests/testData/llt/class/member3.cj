// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    if (Boo().d != 1) {
        return 1
    }
    return 0
}

class Boo {
    public var d: Int32 = {=> 1}
}

/* SCAN
error: mismatched types
  ==> member3.cj:17:27:
   | 
17 |     public var d: Int32 = {=> 1}
   |                           ^^^^^^ expected 'Int32', found '() -> Int64'
   | 

1 error generated, 1 error printed.
*/
