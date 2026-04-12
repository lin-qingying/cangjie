// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class A<T> {
    public func test(a: T, b!: Int64 = 1) {}
}

class B<T> <: A<T> {
    public func test(a: T) {}
    public func i(input: T) {
        var I = test(input)
        return I
    }
}

main() {
    return 0
}

/* SCAN
error: ambiguous match for function call 'test'
  ==> call15.cj:16:17:
   | 
16 |         var I = test(input)
   |                 ^ 
   | 
note: found candidate
  ==> call15.cj:10:17:
   | 
10 |     public func test(a: T, b!: Int64 = 1) {}
   |                 ^ 
   | 
note: found candidate
  ==> call15.cj:14:17:
   | 
14 |     public func test(a: T) {}
   |                 ^ 
   | 

1 error generated, 1 error printed.
*/
