// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class Data
    public var a : Int32  = 100
    public var b :)Float32
    public init()  {
        b = 3var a : Int32  c run(): Int64 {
    var value : Data = Data();
    if ((value.a == 100) && (value.b == 3.0)) {        return 0
    }
    else {
        return 1
    }
}

main(): Int64 {
    return run()
}

/* SCAN
error: unknown suffix 'var' for number literal
  ==> err_syntax_error_00.cj:13:14:
   | 
13 |         b = 3var a : Int32  c run(): Int64 {
   |              ^^^ unknown suffix 'var'
   | 

1 error generated, 1 error printed.
*/
