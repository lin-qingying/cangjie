// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %f %cmp_opt -Woff all

@Annotation[target: [GlobalFunction]]
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

class S<T> {
    @APILevel[1]
    func foo() {}

    @APILevel[1]
    var c = 3
}

enum E<T> {
    | @APILevel[1]
    My(Int32)

    @APILevel[1]
    static func foo<R>() {}
}

main() {
    S<Int32>().foo()
    S<Int64>().foo()
    E<Int32>.foo<Int32>()
    E<Int64>.foo<String>()
}

/* SCAN
error: '@APILevel' not applicable to member function
  ==> instantiated.cj:18:5:
   | 
18 |     @APILevel[1]
   |     ^^^^^^^^^^^^ 
   | 

error: '@APILevel' not applicable to member variable
  ==> instantiated.cj:21:5:
   | 
21 |     @APILevel[1]
   |     ^^^^^^^^^^^^ 
   | 

error: '@APILevel' not applicable to enum constructor
  ==> instantiated.cj:26:7:
   | 
26 |     | @APILevel[1]
   |       ^^^^^^^^^^^^ 
   | 

error: '@APILevel' not applicable to member function
  ==> instantiated.cj:29:5:
   | 
29 |     @APILevel[1]
   |     ^^^^^^^^^^^^ 
   | 

4 errors generated, 4 errors printed.
*/
