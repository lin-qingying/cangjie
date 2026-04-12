// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class d {
    public func test() {}
    public var b = test
}

main() {
    var a = d()
    var b = a.test
    return 0
}

/* SCAN
error: 'test' is not allowed to be accessed before all member variables are initialized
  ==> class9.cj:11:20:
   | 
11 |     public var b = test
   |                    ^ 
   | 

1 error generated, 1 error printed.
*/
