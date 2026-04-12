// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct A {
    public mut prop b: Int64 {
        get() {
            1
        }
        set(v) {}
    }
}

extend A {
    public func b() {}
}

main() {
    var x = A().b
    return 0
}

/* SCAN
error: extend member 'b' is not allowed to shadow members of 'Struct-A'
  ==> extend_property9.cj:19:17:
   | 
19 |     public func b() {}
   |                 ^ 
   | 
note: shadowed definition of 'b' is here
  ==> extend_property9.cj:10:21:
   | 
10 |     public mut prop b: Int64 {
   |                     ^ 
   | 

1 error generated, 1 error printed.
*/
