// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %n.cj -o %output

class C1 {
    public var x = 0
}

extend C1 {
    public func foo1(): This {
        return foo2()
    }

    public func foo2(): This {
        return foo3()
    }

    public func foo3(): This {
        x = 1
        return this
    }
}

main() {
    var obj = C1()
    if (obj.foo1().x == 1) {
        return 0
    }
    return 1
}

/* SCAN
error: 'This' type is not allowed
  ==> class_extend_thistype_ok_2.cj:14:25:
   | 
14 |     public func foo1(): This {
   |                         ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

error: 'This' type is not allowed
  ==> class_extend_thistype_ok_2.cj:18:25:
   | 
18 |     public func foo2(): This {
   |                         ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

error: 'This' type is not allowed
  ==> class_extend_thistype_ok_2.cj:22:25:
   | 
22 |     public func foo3(): This {
   |                         ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

3 errors generated, 3 errors printed.
*/
