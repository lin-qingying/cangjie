// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct B<X,Y> {
    public B( x: Int64, y: X, private var z: Y) {}
    public init(z: Y){
        this.z = z
    }
}

struct C {
    private C( var z: Int64 = 3){ }
}

struct D {
    protected D( var z!: Int64 = 3){ }
}

struct E {
     public E( protected var z!: Int64 = 3){ }
}

struct F {
     public F( static var z!: Int64 = 3){ }
}

struct G{
    public A(var z: Int64 = 3) {}
}

struct H{
    public H(var z: Int64 = 3) {}
    public H(var x: String = "3") {}
}

struct J{
    public J(z: Int64, var z: Int64 = 1)
}

struct K{
    public K(var k: Int64, var k: Int64 = 1)
}

struct L{
    public L(var z: Int64 = 3){
      this("ww")
    }
    public init(a:String = "1"){
       this.z = "a"
    }
}

class M {
     public M  // class's constructor or finalizer should have function body
}

struct N {
     public N( private z: Int64 = 3){ }
}

main():Int64 {
    B<Int64,Int64>(4).z

    C()
    D()
    E()
    F()
    return 0
}

/* SCAN
error: expected ',' or ')', found '='
  ==> primaryConstructor2.cj:17:29:
   | 
17 |     private C( var z: Int64 = 3){ }
   |                             ^ expected ',' or ')' here
   | 

error: unexpected modifier 'static' on variable declaration in primary constructor
  ==> primaryConstructor2.cj:29:16:
   | 
29 |      public F( static var z!: Int64 = 3){ }
   |             ~  ^^^^^^ ~~~ on variable declaration
   |             |  |
   |             |  unexpected modifier
   |             |
   |             in primary constructor
   | 

error: expected declaration, found 'A'
  ==> primaryConstructor2.cj:33:12:
   | 
33 |     public A(var z: Int64 = 3) {}
   |            ^ expected declaration here
   | 
   # help: probably you want a primary constructor name: 'G'

error: expected ',' or ')', found '='
  ==> primaryConstructor2.cj:33:27:
   | 
33 |     public A(var z: Int64 = 3) {}
   |                           ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor2.cj:37:27:
   | 
37 |     public H(var z: Int64 = 3) {}
   |                           ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor2.cj:38:28:
   | 
38 |     public H(var x: String = "3") {}
   |                            ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor2.cj:42:37:
   | 
42 |     public J(z: Int64, var z: Int64 = 1)
   |                                     ^ expected ',' or ')' here
   | 

error: body of main constructor 'J' is missing
  ==> primaryConstructor2.cj:42:41:
   | 
42 |     public J(z: Int64, var z: Int64 = 1)
   |                                         ^ missing main constructor body here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor2.cj:46:41:
   | 
46 |     public K(var k: Int64, var k: Int64 = 1)
   |                                         ^ expected ',' or ')' here
   | 

error: body of main constructor 'K' is missing
  ==> primaryConstructor2.cj:46:45:
   | 
46 |     public K(var k: Int64, var k: Int64 = 1)
   |                                             ^ missing main constructor body here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor2.cj:50:27:
   | 
50 |     public L(var z: Int64 = 3){
   |                           ^ expected ',' or ')' here
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor2.cj:53:26:
   | 
53 |     public init(a:String = "1"){
   |                          ^ expected ',' or ')' here
   | 

error: expected declaration, found 'M'
  ==> primaryConstructor2.cj:59:13:
   | 
59 |      public M  // class's constructor or finalizer should have function body
   |             ^ expected declaration here
   | 

error: expected no modifier before non-member variable parameter, found 'private'
  ==> primaryConstructor2.cj:63:16:
   | 
63 |      public N( private z: Int64 = 3){ }
   |                ^^^^^^^ ~~~~~~~~~~~~ before non-member variable parameter
   |                |
   |                expected no modifier here
   | 
help: if you want an immutable member variable, use 'let':
   | 
63 |      public N( private let z: Int64 = 3){ }
   |                        ~~~~~~~~~~~~~~~~ 
   | 
help: or 'var' to get a mutable member variable:
   | 
63 |      public N( private var z: Int64 = 3){ }
   |                        ~~~~~~~~~~~~~~~~ 
   | 

error: expected ',' or ')', found '='
  ==> primaryConstructor2.cj:63:33:
   | 
63 |      public N( private z: Int64 = 3){ }
   |                                 ^ expected ',' or ')' here
   | 

15 errors generated, 15 errors printed.
*/
