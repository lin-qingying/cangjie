// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Test for option type inference
interface I {}

class A <: I {
    func test(): Unit {
        let a: ?A = Some(A())
        let v0: ??I = Some(a) // error
        let v1: ???I = Some(a) // error
    }
}

main() {}

/* SCAN
error: unable to infer generic argument of this function
  ==> infer_option_02.cj:15:23:
   | 
15 |         let v0: ??I = Some(a) // error
   |                       ^^^^ 
   | 
note: following constraints for type variable 'Generics-T' cannot be solved:
      'Enum-Option<Class-A> <: Generics-T'
      'Generics-T <: Enum-Option<Interface-I>'
  ==> (package std.core)option.cj:11:20:
note: constraint 'Enum-Option<Class-A> <: Generics-T' may come from:
  ==> infer_option_02.cj:15:28:
   | 
15 |         let v0: ??I = Some(a) // error
   |                            ^ of type 'Enum-Option<Class-A>', should match parameter type 'Generics-T'
   | 
note: constraint 'Generics-T <: Enum-Option<Interface-I>' may come from:
  ==> infer_option_02.cj:15:23:
   | 
15 |         let v0: ??I = Some(a) // error
   |                       ^^^^^^^ of return type 'Enum-Option<Generics-T>', should match expected type 'Enum-Option<Enum-Option<Interface-I>>'
   | 

error: mismatched types
  ==> infer_option_02.cj:16:24:
   | 
16 |         let v1: ???I = Some(a) // error
   |                        ^^^^^^^ expected 'Enum-Option<Enum-Option<Enum-Option<Interface-I>>>', found 'Enum-Option<Enum-Option<Class-A>>'
   | 

2 errors generated, 2 errors printed.
*/
