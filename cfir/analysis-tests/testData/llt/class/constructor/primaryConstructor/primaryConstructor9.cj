// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class Data {
    public Data(let a!: Bool = du, var du!: Bool = a) {}
}

class A {
    public let a: Bool
    public var du: Bool
    public init(a!: Bool = du, du!: Bool = a) {
        this.a = a
        this.du = du
    }
}

class Date {
    public var v: Int32 = 1
    public Date(let width!: Int32 = 1, let length!: Int32 = v,
        let perimeter!: () -> Int32 = {
            =>
            var res: Int32 = 1;
            return res
        }) {}
}

class Date1 {
    public var v: Int32 = 1
    public Date1(let width!: Int32 = 1, let length!: Int32 = 2,
        let perimeter!: () -> Int32 = {
            =>
            var res: Int32 = v;
            return res
        }) {}
}

open class Date2 {
    public Date2(let v!: Int32 = 1, let length!: Int32 = 2,
        let perimeter!: () -> Int32 = {
            =>
            var res: Int32 = 1;
            return res
        }) {}
}

class Date3 <: Date2 {
    public Date3(let width!: Int32 = 1, let length!: Int32 = v,
        let perimeter!: () -> Int32 = {
            =>
            var res: Int32 = v;
            return res
        }) {}
}

class B {
    public B(let ai!: Bool = true && xa, var xa!: Bool = ai) {}
}

class C {
    public C(var xa!: Bool = true, let ai!: Bool = xa && true, var c!: Int32 = 3, var x!: Int32 = 2 + Date2().v + this.c) {}
}

/* SCAN
error: 'du' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor9.cj:10:32:
   | 
10 |     public Data(let a!: Bool = du, var du!: Bool = a) {}
   |                                ^ 
   | 

error: 'du' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor9.cj:16:28:
   | 
16 |     public init(a!: Bool = du, du!: Bool = a) {
   |                            ^ 
   | 

error: 'v' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor9.cj:24:61:
   | 
24 |     public Date(let width!: Int32 = 1, let length!: Int32 = v,
   |                                                             ^ 
   | 

error: 'v' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor9.cj:37:30:
   | 
37 |             var res: Int32 = v;
   |                              ^ 
   | 

error: 'v' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor9.cj:52:62:
   | 
52 |     public Date3(let width!: Int32 = 1, let length!: Int32 = v,
   |                                                              ^ 
   | 

error: 'v' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor9.cj:55:30:
   | 
55 |             var res: Int32 = v;
   |                              ^ 
   | 

error: 'xa' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor9.cj:61:38:
   | 
61 |     public B(let ai!: Bool = true && xa, var xa!: Bool = ai) {}
   |                                      ^ 
   | 

error: 'this' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor9.cj:65:115:
   | 
65 |     public C(var xa!: Bool = true, let ai!: Bool = xa && true, var c!: Int32 = 3, var x!: Int32 = 2 + Date2().v + this.c) {}
   |                                                                                                                   ^ 
   | 

error: the variable 'length' must not shadow a member variable of the supertype
  ==> primaryConstructor9.cj:52:41:
   | 
52 |     public Date3(let width!: Int32 = 1, let length!: Int32 = v,
   |                                         ^ 
   | 

error: the variable 'perimeter' must not shadow a member variable of the supertype
  ==> primaryConstructor9.cj:53:9:
   | 
53 |         let perimeter!: () -> Int32 = {
   |         ^ 
   | 

warning: the variable 'du' actually captures this decl primaryConstructor9.cj:10:36, can not captures the decl primaryConstructor9.cj:10:36
  ==> primaryConstructor9.cj:10:32:
   | 
10 |     public Data(let a!: Bool = du, var du!: Bool = a) {}
   |                                ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff semantics`

warning: the variable 'du' actually captures this decl primaryConstructor9.cj:15:5, can not captures the decl primaryConstructor9.cj:16:32
  ==> primaryConstructor9.cj:16:28:
   | 
16 |     public init(a!: Bool = du, du!: Bool = a) {
   |                            ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff semantics`

warning: the variable 'xa' actually captures this decl primaryConstructor9.cj:61:42, can not captures the decl primaryConstructor9.cj:61:42
  ==> primaryConstructor9.cj:61:38:
   | 
61 |     public B(let ai!: Bool = true && xa, var xa!: Bool = ai) {}
   |                                      ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff semantics`

error: 'main' is missing
 ==> primaryConstructor9.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

11 errors generated, 11 errors printed.
3 warnings generated, 3 warnings printed.
*/
