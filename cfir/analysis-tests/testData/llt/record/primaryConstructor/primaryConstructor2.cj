// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct B<X,Y> {
    public B( x: Int64, y: X, private var z: Y) {}
    public init(z: Y){
        this.z = z
    }
}

struct C {
    private C( var z: Int64 <!parse_expected_dot_lparen!>=<!> 3){ }
}

struct D {
    protected D( var z!: Int64 = 3){ }
}

struct E {
     public E( protected var z!: Int64 = 3){ }
}

struct F {
     public F( <!parse_illegal_modifier_in_scope!>static<!> var z!: Int64 = 3){ }
}

struct G{
    public <!parse_expected_decl!>A<!>(var z: Int64 <!parse_expected_dot_lparen!>=<!> 3) {}
}

struct H{
    public H(var z: Int64 <!parse_expected_dot_lparen!>=<!> 3) {}
    public H(var x: String <!parse_expected_dot_lparen!>=<!> "3") {}
}

struct J{
    public J(z: Int64, var z: Int64 <!parse_expected_dot_lparen!>=<!> 1)<!parse_missing_body!>
<!>}

struct K{
    public K(var k: Int64, var k: Int64 <!parse_expected_dot_lparen!>=<!> 1)<!parse_missing_body!>
<!>}

struct L{
    public L(var z: Int64 <!parse_expected_dot_lparen!>=<!> 3){
      this("ww")
    }
    public init(a:String <!parse_expected_dot_lparen!>=<!> "1"){
       this.z = "a"
    }
}

class M {
     public <!parse_expected_decl!>M<!>  // class's constructor or finalizer should have function body
}

struct N {
     public N( <!parse_expected_no_modifier!>private<!> z: Int64 <!parse_expected_dot_lparen!>=<!> 3){ }
}

main():Int64 {
    B<Int64,Int64>(4).z

    C()
    D()
    E()
    F()
    return 0
}
