// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A<T> {}

extend<T> A<T> where T <: String {
    func test() {
        2
    }
}

extend<T> A<T> {
    func foo() {
        return test()
    }
}

main() {
}

/* SCAN
error: the upper bound 'Struct-String' of generic parameter 'Generics-T' must be class or interface
  ==> extend_type_args_02_err.cj:11:8:
   | 
11 | extend<T> A<T> where T <: String {
   |        ^ 
   | 

error: undeclared identifier 'test'
  ==> extend_type_args_02_err.cj:19:16:
   | 
19 |         return test()
   |                ^ 
   | 

2 errors generated, 2 errors printed.
*/
