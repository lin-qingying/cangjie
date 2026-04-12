// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I {
    mut static prop p1: Int64
    mut prop p2: Int64
    static prop p3: Int64
    prop p4: Int64
    mut static prop p5: Int32
}

class A {}

extend A <: I {
    static prop p1: Int64 {
        get() {
            0
        }
    }
    prop p2: Int64 {
        get() {
            0
        }
    }
    mut static prop p3: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    mut prop p4: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    static prop p5: Int64 {
        get() {
            0
        }
    }
}

/* SCAN
error: property 'p1' should have 'mut' modifier
  ==> property_mut_immut_mismatch.cj:20:5:
   | 
20 |       static prop p1: Int64 {
   |  _____^
21 | |         get() {
   | | ...
24 | |     }
   | |_____^
   | 

error: a deriving member must be at least as visible as its base member
  ==> property_mut_immut_mismatch.cj:20:17:
   | 
20 |     static prop p1: Int64 {
   |                 ^^ the visibility of the deriving 'p1' is 'internal'
   | 
note: the visibility of the base 'p1' is 'public'
  ==> property_mut_immut_mismatch.cj:10:21:
   | 
10 |     mut static prop p1: Int64
   |                     ^^ the base 'p1' is in 'interface'
   | 

error: property 'p2' should have 'mut' modifier
  ==> property_mut_immut_mismatch.cj:25:5:
   | 
25 |       prop p2: Int64 {
   |  _____^
26 | |         get() {
   | | ...
29 | |     }
   | |_____^
   | 

error: a deriving member must be at least as visible as its base member
  ==> property_mut_immut_mismatch.cj:25:10:
   | 
25 |     prop p2: Int64 {
   |          ^^ the visibility of the deriving 'p2' is 'internal'
   | 
note: the visibility of the base 'p2' is 'public'
  ==> property_mut_immut_mismatch.cj:11:14:
   | 
11 |     mut prop p2: Int64
   |              ^^ the base 'p2' is in 'interface'
   | 

error: property 'p3' should be immutable
  ==> property_mut_immut_mismatch.cj:30:5:
   | 
30 |       mut static prop p3: Int64 {
   |  _____^
31 | |         get() {
   | | ...
35 | |     }
   | |_____^
   | 

error: a deriving member must be at least as visible as its base member
  ==> property_mut_immut_mismatch.cj:30:21:
   | 
30 |     mut static prop p3: Int64 {
   |                     ^^ the visibility of the deriving 'p3' is 'internal'
   | 
note: the visibility of the base 'p3' is 'public'
  ==> property_mut_immut_mismatch.cj:12:17:
   | 
12 |     static prop p3: Int64
   |                 ^^ the base 'p3' is in 'interface'
   | 

error: property 'p4' should be immutable
  ==> property_mut_immut_mismatch.cj:36:5:
   | 
36 |       mut prop p4: Int64 {
   |  _____^
37 | |         get() {
   | | ...
41 | |     }
   | |_____^
   | 

error: a deriving member must be at least as visible as its base member
  ==> property_mut_immut_mismatch.cj:36:14:
   | 
36 |     mut prop p4: Int64 {
   |              ^^ the visibility of the deriving 'p4' is 'internal'
   | 
note: the visibility of the base 'p4' is 'public'
  ==> property_mut_immut_mismatch.cj:13:10:
   | 
13 |     prop p4: Int64
   |          ^^ the base 'p4' is in 'interface'
   | 

error: The type of the override/implement property must be the same
  ==> property_mut_immut_mismatch.cj:42:5:
   | 
42 |     static prop p5: Int64 {
   |     ^ 
   | 

error: property 'p5' should have 'mut' modifier
  ==> property_mut_immut_mismatch.cj:42:5:
   | 
42 |       static prop p5: Int64 {
   |  _____^
43 | |         get() {
   | | ...
46 | |     }
   | |_____^
   | 

error: a deriving member must be at least as visible as its base member
  ==> property_mut_immut_mismatch.cj:42:17:
   | 
42 |     static prop p5: Int64 {
   |                 ^^ the visibility of the deriving 'p5' is 'internal'
   | 
note: the visibility of the base 'p5' is 'public'
  ==> property_mut_immut_mismatch.cj:14:21:
   | 
14 |     mut static prop p5: Int32
   |                     ^^ the base 'p5' is in 'interface'
   | 

error: 'main' is missing
 ==> property_mut_immut_mismatch.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

12 errors generated, 12 errors printed.
*/
