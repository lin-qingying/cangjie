// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class C1 {

}

extend C1 {
    public func foo(): This {
        return C1()  
    }
}

main() {
    return 1
}

/* SCAN
error: 'This' type is not allowed
  ==> class_extend_thistype_invalid_4.cj:14:24:
   | 
14 |     public func foo(): This {
   |                        ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

1 error generated, 1 error printed.
*/
