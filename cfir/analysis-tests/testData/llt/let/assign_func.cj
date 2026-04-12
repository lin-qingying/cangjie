// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f 2>&1 | compare %f

struct Test {
    public let a: Int32 = 0
    public var b: Int32 = 0
    public static var b2: Int32 = 0
    public func foo(): Int32 {
        return a
    }

    public static func foo1(): Int32 {
        return 0
    }
}

var test = Test()

func a(): Int32 {
    return 5
}

class A {
    public func foo(): Int32 {
        return 1
    }

    public static func foo1(): Int32 {
        return 0
    }
}

var testA = A()

func run(): Int64 {
    test.b = 1
    test.foo = a
    Test.foo1 = a
    testA.foo = a
    A.foo1 = a
    return Int64(test.b + test.foo() - 6)
}

main(): Int64 {
    return run()
}

/* SCAN
error: 'func' can not be assigned
  ==> assign_func.cj:42:5:
   | 
42 |     test.foo = a
   |     ^^^^^^^^ 
   | 

error: 'func' can not be assigned
  ==> assign_func.cj:43:5:
   | 
43 |     Test.foo1 = a
   |     ^^^^^^^^^ 
   | 

error: 'func' can not be assigned
  ==> assign_func.cj:44:5:
   | 
44 |     testA.foo = a
   |     ^^^^^^^^^ 
   | 

error: 'func' can not be assigned
  ==> assign_func.cj:45:5:
   | 
45 |     A.foo1 = a
   |     ^^^^^^ 
   | 

4 errors generated, 4 errors printed.
*/
