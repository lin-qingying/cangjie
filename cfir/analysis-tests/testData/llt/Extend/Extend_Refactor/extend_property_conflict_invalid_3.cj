// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {
    prop foo: Int64 {
        get() {
            return 1
        }
    }
}

interface I2 {
    prop foo: Int64 {
        get() {
            return 2
        }
    }
}

class C1 {}

extend C1 {
    public func foo(): Int64 {
        3
    }
}

extend C1 <: I1 {}

extend C1 <: I2 {}

main() {
    return -1
}

/* SCAN
error: function member 'foo' cannot have the same name with property member in extended type
  ==> extend_property_conflict_invalid_3.cj:28:17:
   | 
28 |     public func foo(): Int64 {
   |                 ^^^ 
   | 
note: found conflict here
  ==> extend_property_conflict_invalid_3.cj:18:10:
   | 
18 |     prop foo: Int64 {
   |          ^^^ 
   | 

error: interface property 'foo' must be implemented in 'extend C1'
  ==> extend_property_conflict_invalid_3.cj:33:1:
   | 
33 | extend C1 <: I1 {}
   | ^ 
   | 

error: interface property 'foo' must be implemented in 'extend C1'
  ==> extend_property_conflict_invalid_3.cj:35:1:
   | 
35 | extend C1 <: I2 {}
   | ^ 
   | 

3 errors generated, 3 errors printed.
*/
