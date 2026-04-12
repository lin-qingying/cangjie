// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func getHeight(height: Int64): Int64 {
    return height * 2
}

func foo<T>(a: T): Int32 {
    return 10
}

class Data<T> {
    public let width: T
    public let f: Int32
    public init(x: T) {
        width = T
        width = ((T))
        f = foo<T>(T)
    }
}

func run(): Int32 {
    let data: Data<(Int64) -> Int64> = Data<(Int64) -> Int64>(getHeight)
    var res = data.width(5)
    if (res == 10) {
        return 0
    }
    return 1
}

main(): Int32 {
    return run()
}

/* SCAN
error: expected member name or constructor call after 'T' type name
  ==> class4_test.cj:21:17:
   | 
21 |         width = T
   |                 ^ 
   | 
note: found candidate
  ==> class4_test.cj:17:12:
   | 
17 | class Data<T> {
   |            ^ 
   | 

error: expected member name or constructor call after 'T' type name
  ==> class4_test.cj:22:19:
   | 
22 |         width = ((T))
   |                   ^ 
   | 
note: found candidate
  ==> class4_test.cj:17:12:
   | 
17 | class Data<T> {
   |            ^ 
   | 

error: expected member name or constructor call after 'T' type name
  ==> class4_test.cj:23:20:
   | 
23 |         f = foo<T>(T)
   |                    ^ 
   | 
note: found candidate
  ==> class4_test.cj:17:12:
   | 
17 | class Data<T> {
   |            ^ 
   | 

error: cannot assign to immutable value
  ==> class4_test.cj:22:9:
   | 
22 |         width = ((T))
   |         ^^^^^^^^^^^^^ 
   | 
note: variable 'width' is immutable
  ==> class4_test.cj:18:16:
   | 
18 |     public let width: T
   |                ^^^^^ 
   | 

4 errors generated, 4 errors printed.
*/
