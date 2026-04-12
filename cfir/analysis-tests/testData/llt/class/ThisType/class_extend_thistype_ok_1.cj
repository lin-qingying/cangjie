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
    public func foo(): This {
        x = 1
        return this
    }
}

main() {
    var obj = C1()
    if (obj.foo().x == 1) {
        return 0
    }
    return 1
}

/* SCAN
error: 'This' type is not allowed
  ==> class_extend_thistype_ok_1.cj:14:24:
   | 
14 |     public func foo(): This {
   |                        ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

1 error generated, 1 error printed.
*/
