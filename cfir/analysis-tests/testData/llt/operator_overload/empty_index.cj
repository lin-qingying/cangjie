// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

struct LA{
  public var a : (Array<Bool>, Array<Bool>)
  public init(input:Bool){
    a = ([input],[input])
  }
}

main(): Int64 {
    var l_a = LA(false)
    var r_a = true
    var r_b = true
    l_a.a[0][] = r_a || r_b
 0   if (l_a.a[0][0] == true) {
        return 0
    }
    return 1
}

/* SCAN
error: expected expression after '[', found ']'
  ==> empty_index.cj:20:14:
   | 
20 |     l_a.a[0][] = r_a || r_b
   |              ^ expected expression here
   | 

error: expected ';' or '<NL>', found keyword 'if'
  ==> empty_index.cj:21:6:
   | 
21 |  0   if (l_a.a[0][0] == true) {
   |      ^^ expected ';' or '<NL>' here
   | 

2 errors generated, 2 errors printed.
*/
