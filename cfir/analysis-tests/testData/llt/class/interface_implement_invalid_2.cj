// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

open class Base {}

class Derived <: Base {}

interface I1 {
    func foo(): Base
}

abstract class C1 <: I1 {}

class C2 <: I1 {}

class C3 <: I1 { // expected: regex class 'C3' missing abstract modifier or abstract function 'foo' should be implemented
    public func foo(): Int64 { // expected: regex The return type of the override/implement function must be the same as or a subtype of the overridden/implemented function
        return 0
    }
}

class C4 <: I2 {
    public func foo(): Base {
        return Base()
    }
}

class C5 <: I2 {
    public func foo(): Derived {
        return Derived()
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: undeclared type name 'I2'
  ==> interface_implement_invalid_2.cj:27:13:
   | 
27 | class C4 <: I2 {
   |             ^ 
   | 

error: undeclared type name 'I2'
  ==> interface_implement_invalid_2.cj:33:13:
   | 
33 | class C5 <: I2 {
   |             ^ 
   | 

error: class 'C2' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> interface_implement_invalid_2.cj:19:1:
   | 
19 | class C2 <: I1 {}
   | ^ 
   | 
note: unimplemented interface function 'foo'
  ==> interface_implement_invalid_2.cj:14:5:
   | 
14 |     func foo(): Base
   |     ^ 
   | 

error: return type of 'foo' is not identical or not a subtype of the overridden/redefined/implement function
  ==> interface_implement_invalid_2.cj:22:17:
   | 
22 |     public func foo(): Int64 { // expected: regex The return type of the override/implement function must be the same as or a subtype of the overridden/implemented function
   |                 ^^^ 
   | 
note: 'Int64' is not a subtype of 'Class-Base'
  ==> interface_implement_invalid_2.cj:14:10:
   | 
14 |     func foo(): Base
   |          ^^^ 
   | 

4 errors generated, 4 errors printed.
*/
