// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class Data {
    public Data(let a!: Bool = x, var x!: Bool = a) {}
}

func run(): Int32 {
    var data: Data = Data()
    print(data.a.toString())
    print(data.x.toString())

    if (data.a && data.x) {
        return 0
    }
    return 1
}

main(): Int64 {
    return Int64(run())
}

/* SCAN
error: 'x' is not allowed to be used in default parameter value of the constructor
  ==> primaryConstructor10.cj:10:32:
   | 
10 |     public Data(let a!: Bool = x, var x!: Bool = a) {}
   |                                ^ 
   | 

warning: the variable 'x' actually captures this decl primaryConstructor10.cj:10:35, can not captures the decl primaryConstructor10.cj:10:35
  ==> primaryConstructor10.cj:10:32:
   | 
10 |     public Data(let a!: Bool = x, var x!: Bool = a) {}
   |                                ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff semantics`

1 error generated, 1 error printed.
1 warning generated, 1 warning printed.
*/
