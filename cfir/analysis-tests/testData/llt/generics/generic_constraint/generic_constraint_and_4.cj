// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class C {
    public func foo(a: Int32) {
        return a
    }
}

interface I<T> {
    func foo(a: T) {
        return a
    }
}

interface I2<T> {
    func foo(a: T) {
        return a
    }
}

func Foo<U>(a: U) where U <: C & I<U> & I2<U> {
    var b: U = a.foo(a)
    var c = a.foo(1, 2)
    var d = a.bar()
    return b
}

class C1 <: C & I<C1> & I2<C1> {}

main() {
    var a: C1 = Foo<C1>(C1())
    return 0
}

/* SCAN
error: extra argument given for parameter list '(Generics-T)' in call
  ==> generic_constraint_and_4.cj:29:18:
   | 
29 |     var c = a.foo(1, 2)
   |                  ^^^^^^ expected 1 argument, found 2
   | 
note: found candidate
  ==> generic_constraint_and_4.cj:16:10:
   | 
16 |     func foo(a: T) {
   |          ^^^ 
   | 

error: 'bar' is not found for generic type 'U' in its upper bounds
  ==> generic_constraint_and_4.cj:30:15:
   | 
30 |     var d = a.bar()
   |               ^^^ 
   | 

error: interface function 'foo' must be implemented in 'C1'
  ==> generic_constraint_and_4.cj:34:1:
   | 
34 | class C1 <: C & I<C1> & I2<C1> {}
   | ^ 
   | 

3 errors generated, 3 errors printed.
*/
