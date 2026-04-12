// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class Basei {
    public func getNum1() {
        return 1
    }
    public var c = 5;
    private var t = 3
}

class Data <: Basei {
    public var a = 2;
    public var resc = temp;
    public var res = getNum1();
    public init() {}
}

func run(): Int64 {
    var value: Data = Data()
}

/* SCAN
error: undeclared identifier 'temp'
  ==> class3.cj:19:23:
   | 
19 |     public var resc = temp;
   |                       ^ 
   | 

error: mismatched types
  ==> class3.cj:25:5:
   | 
25 |     var value: Data = Data()
   |     ^^^^^^^^^^^^^^^^^^^^^^^^ expected 'Int64', found 'Unit'
   | 
   # note: definitions and declarations are always of type 'Unit'

error: 'getNum1' is not allowed to be accessed before all member variables are initialized
  ==> class3.cj:20:22:
   | 
20 |     public var res = getNum1();
   |                      ^ 
   | 

error: 'main' is missing
 ==> class3.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

4 errors generated, 4 errors printed.
*/
