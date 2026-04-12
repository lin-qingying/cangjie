// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

open class A<T>{
    public func test(a:T){print("inside")}
    public var i : T
    public init(a: T) { this.i = a }
    public func b(){
    test(i)
}
return 1;} 

main():Int64{
    A<Int64>(0).b()
    return 0
}

/* SCAN
error: expected declaration, found keyword 'return'
  ==> class_static03.cj:16:1:
   | 
16 | return 1;} 
   | ^^^^^^ expected declaration here
   | 

1 error generated, 1 error printed.
*/
