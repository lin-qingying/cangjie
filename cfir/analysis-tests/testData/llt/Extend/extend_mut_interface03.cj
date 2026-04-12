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

interface I2 <: I {}

extend Int64 <: I2 {}

extend Bool <: I2 {}

extend Unit <: I2 {}

extend Nothing <: I2 {}

extend String <: I2 {}

extend<T> Range<T> <: I2 {}

enum A {
    | AAA
}

extend A <: I2 {}

main() {}

/* SCAN
error: interface 'I' is not able to be extended
  ==> extend_mut_interface03.cj:20:1:
   | 
20 | extend Int64 <: I2 {}
   | ^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface03.cj:22:1:
   | 
22 | extend Bool <: I2 {}
   | ^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface03.cj:24:1:
   | 
24 | extend Unit <: I2 {}
   | ^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface03.cj:26:1:
   | 
26 | extend Nothing <: I2 {}
   | ^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface03.cj:28:1:
   | 
28 | extend String <: I2 {}
   | ^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface03.cj:30:1:
   | 
30 | extend<T> Range<T> <: I2 {}
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> extend_mut_interface03.cj:36:1:
   | 
36 | extend A <: I2 {}
   | ^^^^^^^^^^^^^^^^^ 
   | 
   # note: interface 'I' contains mutable member 'p'

7 errors generated, 7 errors printed.
*/
