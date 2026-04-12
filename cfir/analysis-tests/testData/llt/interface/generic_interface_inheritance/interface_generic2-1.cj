// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f %noColor 2>&1 | compare %f

interface I<T> {
    func foo(a: T): T
}

class C<T> <: I<T> & I<Int32> & I<Float64> {
    public func foo(a: Int32): Int32 {
        return a
    }
    public func foo(a: T): T {
        return a
    }
    public func foo(a: Float64): Float64 {
        return a
    }
}

main() {
    var c = C<Int32>()
    if (c.foo(1) != 1) {
        return 1
    }
    return 0
}

/* SCAN
error: ambiguous match for function call 'foo'
  ==> interface_generic2-1.cj:27:11:
   | 
27 |     if (c.foo(1) != 1) {
   |           ^ 
   | 
note: found candidate
  ==> interface_generic2-1.cj:14:17:
   | 
14 |     public func foo(a: Int32): Int32 {
   |                 ^ 
   | 
note: found candidate
  ==> interface_generic2-1.cj:17:17:
   | 
17 |     public func foo(a: T): T {
   |                 ^ 
   | 

error: class 'C' inherits or implements duplicate interface 'I'
  ==> interface_generic2-1.cj:26:13:
   | 
26 |     var c = C<Int32>()
   |             ^ 
   | 

error: generic instantiation 'C<Int32>' causes ambiguous function 'foo'
  ==> interface_generic2-1.cj:26:13:
   | 
26 |     var c = C<Int32>()
   |             ^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic2-1.cj:14:17:
   | 
14 |     public func foo(a: Int32): Int32 {
   |                 ^^^ 
   | 
note: found candidate
  ==> interface_generic2-1.cj:17:17:
   | 
17 |     public func foo(a: T): T {
   |                 ^^^ 
   | 

3 errors generated, 3 errors printed.
*/
