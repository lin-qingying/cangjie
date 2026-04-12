// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class Data {
    public var a: Int32 = 2
    public var b: Int32
    public init(){
        b = 4
    }
    public init(x: T){
        init()
    }
}

func run(): Int64 {
    let data: Data = Data(55)
    var res1: Int32 = data.a
    var res2: Int32 = data.b
    if (res1 == 2 && res2 == 4) {
    return 0
    }
    return 1
}

main(): Int64 {
    return run()
}

/* SCAN
error: expected expression or declaration, found keyword 'init'
  ==> class10.cj:16:9:
   | 
16 |         init()
   |         ^^^^ expected expression or declaration here
   | 

1 error generated, 1 error printed.
*/
