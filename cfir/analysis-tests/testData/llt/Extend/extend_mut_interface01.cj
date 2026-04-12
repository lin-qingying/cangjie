// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I {
    mut prop p: Int64 {
        get() {
            1
        }
        set(_) {}
    }
}

extend Int64 <: I {}

extend Bool <: I {}

extend Unit <: I {}

extend Nothing <: I {}

extend String <: I {}

extend<T> Range<T> <: I {}

enum A {
    | AAA
}

extend A <: I {}

main() {}

/* SCAN
error: interface 'I' is not able to be extended
  ==> extend_mut_interface01.cj:18:1:
   | 
18 | extend Int64 <: I {}
   | ^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface01.cj:20:1:
   | 
20 | extend Bool <: I {}
   | ^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface01.cj:22:1:
   | 
22 | extend Unit <: I {}
   | ^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface01.cj:24:1:
   | 
24 | extend Nothing <: I {}
   | ^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface01.cj:26:1:
   | 
26 | extend String <: I {}
   | ^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface01.cj:28:1:
   | 
28 | extend<T> Range<T> <: I {}
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface01.cj:34:1:
   | 
34 | extend A <: I {}
   | ^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

7 errors generated, 7 errors printed.
*/
