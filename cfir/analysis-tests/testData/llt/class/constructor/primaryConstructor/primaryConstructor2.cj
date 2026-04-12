// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

open class B<X, Y> {
    public B(x: Int64, y: X, private var z: Y) {}
    public init(z: Y) {
        this.z = z
    }
}

class C {
    private C(var z!: Int64 = 3) {}
}

class F {
    public F(var z!: Int64 = 3) {}
}

class G {
    public G(var z!: Int64 = 3) {}
}

class H {
    public H(var z!: Int64 = 3) {}
    public H(var x!: String = "3") {}
}

class J {
    public J(z: Int64, var z!: Int64 = 1) {}
}

class K {
    public K(var k: Int64, var k!: Int64 = 1) {}
}

open class D {
    protected D(var z!: Int64 = 3) {}
}

class E <: D {
    public E(var z!: Int64 = 3) {
        var c: Int64 = 2
        super()
    }
}

class L <: D {
    public L(var z!: Int64 = 3) {
        this("ww")
    }
    public init(a!: String = "1") {
        this.z = "a"
    }
}

class N <: B<Int64, Int64> {
    public N(c: Int64) {}
}

main(): Int64 {
    B<Int64, Int64>(4).z
    C()
    D()
    E()
    F()
    return 0
}

/* SCAN
error: class 'H' cannot have more than one primary constructor
  ==> primaryConstructor2.cj:30:5:
   | 
30 |     public H(var x!: String = "3") {}
   |     ^ 
   | 

error: redefinition of declaration 'z'
  ==> primaryConstructor2.cj:34:28:
   | 
34 |     public J(z: Int64, var z!: Int64 = 1) {}
   |                            ^ 
   | 
note: 'z' is previously declared here
  ==> primaryConstructor2.cj:34:14:
   | 
34 |     public J(z: Int64, var z!: Int64 = 1) {}
   |              ^ 
   | 

error: redefinition of declaration 'k'
  ==> primaryConstructor2.cj:38:32:
   | 
38 |     public K(var k: Int64, var k!: Int64 = 1) {}
   |                                ^ 
   | 
note: 'k' is previously declared here
  ==> primaryConstructor2.cj:38:18:
   | 
38 |     public K(var k: Int64, var k!: Int64 = 1) {}
   |                  ^ 
   | 

error: call to 'super' must be first expression in constructor of class 'E'
  ==> primaryConstructor2.cj:48:9:
   | 
48 |         super()
   |         ^ 
   | 

error: mismatched types
  ==> primaryConstructor2.cj:57:18:
   | 
57 |         this.z = "a"
   |                  ^^^ expected 'Int64', found 'Struct-String'
   | 

error: type incompatible in this assignment
  ==> primaryConstructor2.cj:57:9:
   | 
57 |         this.z = "a"
   |         ^ 
   | 

error: invalid calling 'this' in primary constructor 
  ==> primaryConstructor2.cj:54:9:
   | 
54 |         this("ww")
   |         ^ 
   | 

error: missing argument prefix 'z:' for named parameter
  ==> primaryConstructor2.cj:54:14:
   | 
54 |         this("ww")
   |              ^^^^ 
   | 

error: there is no non-parameter constructor in super class, please invoke super call explicitly
  ==> primaryConstructor2.cj:62:12:
   | 
62 |     public N(c: Int64) {}
   |            ^ 
   | 

error: no matching constructor for call 'C'
  ==> primaryConstructor2.cj:67:5:
   | 
67 |     C()
   |     ^ 
   | 

error: can not access field 'z'
  ==> primaryConstructor2.cj:66:5:
   | 
66 |     B<Int64, Int64>(4).z
   |     ^ 
   | 

error: the uninitialized member variable 'k' is not initialized in the constructor of class or struct
  ==> primaryConstructor2.cj:38:5:
   | 
38 |     public K(var k: Int64, var k!: Int64 = 1) {}
   |     ^ 
   | 

error: the variable 'z' must not shadow a member variable of the supertype
  ==> primaryConstructor2.cj:46:14:
   | 
46 |     public E(var z!: Int64 = 3) {
   |              ^ 
   | 

error: the variable 'z' must not shadow a member variable of the supertype
  ==> primaryConstructor2.cj:53:14:
   | 
53 |     public L(var z!: Int64 = 3) {
   |              ^ 
   | 

14 errors generated, 14 errors printed.
*/
