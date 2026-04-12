// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

public open class A {}

func f(x: Int64): Int64 {
    1
}

main() {
    var d: Bool = 0 |> {
        f: Int64 =>
        A(name: 1);
        A()
    }
    var g = f ~> {
        a: Int64 =>
        A(name: 1);
        return 0
    }
}

/* SCAN
error: extra argument given for parameter list '()' in call
  ==> erro_flow_lambda.cj:18:10:
   | 
18 |         A(name: 1);
   |          ^^^^^^^^^ expected 0 arguments, found 1
   | 

error: extra argument given for parameter list '()' in call
  ==> erro_flow_lambda.cj:23:10:
   | 
23 |         A(name: 1);
   |          ^^^^^^^^^ expected 0 arguments, found 1
   | 

2 errors generated, 2 errors printed.
*/
