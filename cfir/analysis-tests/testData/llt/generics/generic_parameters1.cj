// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class Base<U> {
    public var a: T
    public init(x: T) {
        a = x
    }
}

class Data<T> <: Base<Int32> {
    public var f: T
    public init(input: T) {
        super(0)
        f = input
    }
}

func run(): Int64 {
    var i1: Int64 = 4
    var data: Data<Int64> = Data<Int64>(i1)
    var res: Int64 = data.f
    if (res == 4) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}

/* SCAN
error: undeclared type name 'T'
  ==> generic_parameters1.cj:10:19:
   | 
10 |     public var a: T
   |                   ^ 
   | 

error: undeclared type name 'T'
  ==> generic_parameters1.cj:11:20:
   | 
11 |     public init(x: T) {
   |                    ^ 
   | 

error: no matching function declaration for function call 'super'
  ==> generic_parameters1.cj:19:9:
   | 
19 |         super(0)
   |         ^ 
   | 

3 errors generated, 3 errors printed.
*/
