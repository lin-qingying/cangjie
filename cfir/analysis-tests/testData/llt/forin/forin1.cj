// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %frontendCompiler %typecheck %cmp_opt %f

class Data {
    public var a: Int32
    public var b: Int32
    public var c: Int32
    public init(aa:Int32, bb:Int32, cc:Int32) {
        this.a = aa
        this.b = bb
        this.c = cc
    }
}

func run(): Int64 {
    var value : Data = Data(0,0,0)
    value.a = 2
    if (value.a == 0) {
        return 1;
    }
    return 0;
}

main(): Int64 {
    for
    return run() 
}

/* SCAN-IN
error: expected '(', found keyword 'return'
  ==> forin1.cj:31:5:
   | 
31 |     return run() 
   |     ^^^^^^ expected '(' here
   | 
*/
