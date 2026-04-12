// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class C1 {
    public var i: Int64 = 0
}

extend C1 {
    public mut func foo() {
        this.i = 1
    }
}

main() {
    return 1
}

/* SCAN
error: 'mut' modifier is illegal in extend body of 'C1'
  ==> record_extend_mut_invalid_1.cj:14:12:
   | 
14 |     public mut func foo() {
   |            ^^^ 
   | 

1 error generated, 1 error printed.
*/
