// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

enum e<T> {
    | A(Int32)
    | B(Int32, Int32)

    public func foo(a: T) {
        1
    }
}

extend<T> e<T> {
    public func foo(a: Int32) {
        0
    }
}

main() {
    let a = e<Int32>.A(1)
    return 0
}

/* SCAN
error: generic instantiation 'e<Int32>' causes ambiguous function 'foo'
  ==> extend_instantiation_function_ambiguous3.cj:25:13:
   | 
25 |     let a = e<Int32>.A(1)
   |             ^^^^^^^^^^ 
   | 
note: found candidate
  ==> extend_instantiation_function_ambiguous3.cj:13:17:
   | 
13 |     public func foo(a: T) {
   |                 ^^^ 
   | 
note: found candidate
  ==> extend_instantiation_function_ambiguous3.cj:19:17:
   | 
19 |     public func foo(a: Int32) {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
