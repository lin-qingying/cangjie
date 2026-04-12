// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {
    protected prop f: String {
        get() {
            return "C"
        }
    }
}

extend A {
    public func f(): Int64 {
        0
    }
}

/* SCAN
error: extend member 'f' is not allowed to shadow members of 'Class-A'
  ==> extend_property1.cj:18:17:
   | 
18 |     public func f(): Int64 {
   |                 ^ 
   | 
note: shadowed definition of 'f' is here
  ==> extend_property1.cj:10:20:
   | 
10 |     protected prop f: String {
   |                    ^ 
   | 

error: 'main' is missing
 ==> extend_property1.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
