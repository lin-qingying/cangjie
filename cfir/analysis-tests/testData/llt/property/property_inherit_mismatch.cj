// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I {
    mut prop a: Int64
    prop b: Int64
}

class A {}

extend A <: I {
    prop a: Int64 {
        get() {
            0
        }
    }
    mut prop b: Int64 {
        get() {
            0
        }
        set(v) {
            ()
        }
    }
}

main() {
    0
}

/* SCAN
error: property 'a' should have 'mut' modifier
  ==> property_inherit_mismatch.cj:17:5:
   | 
17 |       prop a: Int64 {
   |  _____^
18 | |         get() {
   | | ...
21 | |     }
   | |_____^
   | 

error: a deriving member must be at least as visible as its base member
  ==> property_inherit_mismatch.cj:17:10:
   | 
17 |     prop a: Int64 {
   |          ^ the visibility of the deriving 'a' is 'internal'
   | 
note: the visibility of the base 'a' is 'public'
  ==> property_inherit_mismatch.cj:10:14:
   | 
10 |     mut prop a: Int64
   |              ^ the base 'a' is in 'interface'
   | 

error: property 'b' should be immutable
  ==> property_inherit_mismatch.cj:22:5:
   | 
22 |       mut prop b: Int64 {
   |  _____^
23 | |         get() {
   | | ...
29 | |     }
   | |_____^
   | 

error: a deriving member must be at least as visible as its base member
  ==> property_inherit_mismatch.cj:22:14:
   | 
22 |     mut prop b: Int64 {
   |              ^ the visibility of the deriving 'b' is 'internal'
   | 
note: the visibility of the base 'b' is 'public'
  ==> property_inherit_mismatch.cj:11:10:
   | 
11 |     prop b: Int64
   |          ^ the base 'b' is in 'interface'
   | 

4 errors generated, 4 errors printed.
*/
