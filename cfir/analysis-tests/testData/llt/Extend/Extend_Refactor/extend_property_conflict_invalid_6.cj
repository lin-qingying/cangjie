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
    prop foo: Int32 {
        get() {
            return 2
        }
    }
}

class C1 {}

extend C1 <: I1 & I2 {
    public prop foo: Int64 {
        get() {
            return 2
        }
    }
}

main() {
    return -1
}

/* SCAN
error: type of the inherited property members 'foo' are not identical and not in subtype relation
  ==> extend_property_conflict_invalid_6.cj:27:1:
   | 
27 | extend C1 <: I1 & I2 {
   | ^ 
   | 
   # note: conflict types are 'Int32', 'Int64'

error: The type of the override/implement property must be the same
  ==> extend_property_conflict_invalid_6.cj:28:5:
   | 
28 |     public prop foo: Int64 {
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
