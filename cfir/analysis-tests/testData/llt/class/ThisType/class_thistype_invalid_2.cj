// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class C1 {
    public func foo(): This {
        var a: This = this  
        return this
    }
}

main() {
    return 1
}

/* SCAN
error: 'This' type is not allowed
  ==> class_thistype_invalid_2.cj:11:16:
   | 
11 |         var a: This = this  
   |                ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

1 error generated, 1 error printed.
*/
