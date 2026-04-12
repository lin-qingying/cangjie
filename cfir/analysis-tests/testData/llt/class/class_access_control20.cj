// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class A {
    public static func f() {}
}

class B <: A {
    private static redef func f() {
        print(1)
    }
}

main() {
}

/* SCAN
error: a deriving member must be at least as visible as its base member
  ==> class_access_control20.cj:14:31:
   | 
14 |     private static redef func f() {
   |                               ^ the visibility of the deriving 'f' is 'private'
   | 
note: the visibility of the base 'f' is 'public'
  ==> class_access_control20.cj:10:24:
   | 
10 |     public static func f() {}
   |                        ^ 
   | 

1 error generated, 1 error printed.
*/
