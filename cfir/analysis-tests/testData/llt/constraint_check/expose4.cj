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
  ==> expose4.cj:28:18:
   | 
28 |     var c = a.foo(1, 2)
   |                  ^^^^^^ expected 1 argument, found 2
   | 
note: found candidate
  ==> expose4.cj:16:10:
   | 
16 |     func foo(a: T) {
   |          ^^^ 
   | 

error: 'bar' is not found for generic type 'U' in its upper bounds
  ==> expose4.cj:29:15:
   | 
29 |     var d = a.bar()
   |               ^^^ 
   | 

error: undeclared identifier 'b'
  ==> expose4.cj:30:12:
   | 
30 |     return b
   |            ^ 
   | 

error: interface function 'foo' must be implemented in 'C1'
  ==> expose4.cj:33:1:
   | 
33 | class C1 <: C & I<C1> & I2<C1> {}
   | ^ 
   | 

4 errors generated, 4 errors printed.
*/
