// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

open class A {
    static func f<T>() {}
}

class B <: A {
    private static func f<T>() {}
}

main() {}

/* SCAN
error: a deriving member must be at least as visible as its base member
  ==> class_access_control21.cj:14:25:
   | 
14 |     private static func f<T>() {}
   |                         ^ the visibility of the deriving 'f' is 'private'
   | 
note: the visibility of the base 'f' is 'internal'
  ==> class_access_control21.cj:10:17:
   | 
10 |     static func f<T>() {}
   |                 ^ 
   | 

1 error generated, 1 error printed.
*/
