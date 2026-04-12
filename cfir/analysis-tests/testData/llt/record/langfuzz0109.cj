// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

public struct Record10 {
    public let var69: (Record10, Rune) = (Record10(), r'd');
}

main(): Int64 {
    return 0;
}

/* SCAN
error: recursive constructor calling detected
  ==> langfuzz0109.cj:10:43:
   | 
10 |     public let var69: (Record10, Rune) = (Record10(), r'd');
   |                                           ^^^^^^^^ 
   | 
note: depends on
 ==> langfuzz0109.cj:9:15:
  | 
9 | public struct Record10 {
  |               ^^^^^^^^ 
  | 
note: depends on
  ==> langfuzz0109.cj:10:43:
   | 
10 |     public let var69: (Record10, Rune) = (Record10(), r'd');
   |                                           ^^^^^^^^ 
   | 

error: value type recursive detected: 'Record10->Record10'
 ==> langfuzz0109.cj:9:1:
  | 
9 | public struct Record10 {
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
