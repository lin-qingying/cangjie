// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func foo1(): (Float32, String) {}

func foo2(): (Float64, String) {}

main() {}

/* SCAN
error: mismatched types
 ==> join_05_2.cj:9:32:
  | 
9 | func foo1(): (Float32, String) {}
  |              ~~~~~~~~~~~~~~~~~ ^^ expected 'Tuple<Float32, Struct-String>', found 'Unit'
  |              |
  |              expected 'Tuple<Float32, Struct-String>' because of return type
  | 

error: mismatched types
  ==> join_05_2.cj:11:32:
   | 
11 | func foo2(): (Float64, String) {}
   |              ~~~~~~~~~~~~~~~~~ ^^ expected 'Tuple<Float64, Struct-String>', found 'Unit'
   |              |
   |              expected 'Tuple<Float64, Struct-String>' because of return type
   | 

2 errors generated, 2 errors printed.
*/
