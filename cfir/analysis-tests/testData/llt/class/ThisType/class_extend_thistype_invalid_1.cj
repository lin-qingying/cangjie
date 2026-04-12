// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class C1 {
}

extend C1 {
    public func foo(a: This): This { 
        return this
    }
}

main() {
    return 1
}

/* SCAN
error: 'This' type is not allowed
  ==> class_extend_thistype_invalid_1.cj:13:24:
   | 
13 |     public func foo(a: This): This { 
   |                        ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

error: 'This' type is not allowed
  ==> class_extend_thistype_invalid_1.cj:13:31:
   | 
13 |     public func foo(a: This): This { 
   |                               ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

2 errors generated, 2 errors printed.
*/
