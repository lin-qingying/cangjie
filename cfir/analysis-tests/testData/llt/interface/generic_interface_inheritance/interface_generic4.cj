// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

interface I<T> {
    func foo(a: T): T
}

class C<T> <: I<T> & I<Float64> & I<Float64> {
    public func foo(a: T): T {
        return a
    }
    public func foo(a: Float64): Float64 {
        return a
    }
}

main() {
    return 0
}

/* SCAN
error: class 'C' inherits or implements duplicate interface 'I'
  ==> interface_generic4.cj:13:1:
   | 
13 | class C<T> <: I<T> & I<Float64> & I<Float64> {
   | ^ 
   | 

1 error generated, 1 error printed.
*/
