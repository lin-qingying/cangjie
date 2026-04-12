// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class LA{
  public var a : Array<Int32>*Array,Int32>
  public init(a:Int32){
    this.a = ([a],[a])
  }
}

main(): Int64 {
    var l_a = LA(4)
    var r_a : (Int32*Int32) = (2,2)
    l_a.a[0][0] = r_a[0]
    if l_a.a[0][0] == Int32(2) {
    return 0
ord }
    return 1
}

/* SCAN
error: expected ';' or '<NL>', found '*'
  ==> array_invalid_type.cj:10:30:
   | 
10 |   public var a : Array<Int32>*Array,Int32>
   |                              ^ expected ';' or '<NL>' here
   | 

error: unclosed delimiter: '('
  ==> array_invalid_type.cj:18:21:
   | 
18 |     var r_a : (Int32*Int32) = (2,2)
   |               ~     ^ expected ')' here
   |               |
   |               to match this opening '('
   | 

error: expected '(' after 'if', found 'l_a'
  ==> array_invalid_type.cj:20:8:
   | 
20 |     if l_a.a[0][0] == Int32(2) {
   |     ~~ ^^^ expected '(' here
   |     |
   |     after this
   | 

3 errors generated, 3 errors printed.
*/
