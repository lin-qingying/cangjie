// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1 {
    public var i = 0

    public mut func foo(): Unit {
        i += 1
    }

    public var fn = {=> foo()}
}

main() {
    return 1
}

/* SCAN
error: 'foo' is not allowed to be accessed before all member variables are initialized
  ==> record_mut_invalid_13.cj:16:25:
   | 
16 |     public var fn = {=> foo()}
   |                         ^ 
   | 

1 error generated, 1 error printed.
*/
