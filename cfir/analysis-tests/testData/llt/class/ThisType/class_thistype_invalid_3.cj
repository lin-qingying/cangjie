// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class C1 {
    public func foo(): This {
        var a = this
        return a
    }
}

main() {
    return 1
}

/* SCAN
error: mismatched types
  ==> class_thistype_invalid_3.cj:12:16:
   | 
12 |         return a
   |                ^ expected 'This', found 'Class-C1'
   | 

1 error generated, 1 error printed.
*/
