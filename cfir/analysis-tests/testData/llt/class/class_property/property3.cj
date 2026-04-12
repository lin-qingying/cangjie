// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class A {
    public mut prop a: Int64 {
        get() {
            return b
        }
        set(v) {
            return ()
        }
    }
    public var b = a
}

/* SCAN
error: 'a' is not allowed to be accessed before all member variables are initialized
  ==> property3.cj:18:20:
   | 
18 |     public var b = a
   |                    ^ 
   | 

error: 'main' is missing
 ==> property3.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
