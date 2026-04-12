// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface Bar {
    func f(): String
}

struct Foo <: Bar {
    func f(): String {
        "A"
    }
}

open class A {
    public open func f(): Foo {
        return Foo()
    }
}

class B <: A {
    public override func f(): Bar {
        return Foo()
    }
}

func f(): Foo {
    return Foo()
}

main() {
    var a: (Bar, Bar) = (Foo(), Foo())
    let b = (Foo(), Foo())
    a = b
    var c: () -> Bar = f
    return 0
}

/* SCAN
error: mismatched types
  ==> covariance.cj:38:9:
   | 
38 |     a = b
   |         ^ expected 'Tuple<Interface-Bar, Interface-Bar>', found 'Tuple<Struct-Foo, Struct-Foo>'
   | 

error: mismatched types
  ==> covariance.cj:39:24:
   | 
39 |     var c: () -> Bar = f
   |                        ^ expected '() -> Interface-Bar', found '() -> Struct-Foo'
   | 

error: a deriving member must be at least as visible as its base member
  ==> covariance.cj:14:10:
   | 
14 |     func f(): String {
   |          ^ the visibility of the deriving 'f' is 'internal'
   | 
note: the visibility of the base 'f' is 'public'
  ==> covariance.cj:10:10:
   | 
10 |     func f(): String
   |          ^ the base 'f' is in 'interface'
   | 

error: return type of 'f' is not identical or not a subtype of the overridden/redefined/implement function
  ==> covariance.cj:26:26:
   | 
26 |     public override func f(): Bar {
   |                          ^ 
   | 
note: 'Interface-Bar' is not a subtype of 'Struct-Foo'
  ==> covariance.cj:20:22:
   | 
20 |     public open func f(): Foo {
   |                      ^ 
   | 

4 errors generated, 4 errors printed.
*/
