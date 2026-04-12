// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

let a = true
let b: UInt16 = 5

struct c {
    public let d: UInt16
    public func e() {
        match {
            case {=>} => if (true) {
                b
            } else {
                UInt16(2)
            }
            case _ => if (a) {
                d
            } else {
                UInt16(9)
            }
        }
    }
}

/* SCAN
error: mismatched types
  ==> match019.cj:16:18:
   | 
16 |             case {=>} => if (true) {
   |                  ^^^^ expected 'Bool', found '() -> Unit'
   | 

error: the uninitialized member variable 'd' is not initialized in the constructor of class or struct
  ==> match019.cj:13:5:
   | 
13 |     public let d: UInt16
   |     ^ 
   | 

error: 'main' is missing
 ==> match019.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
