// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %overflowOpt %cmp_opt %f -o %output 

public interface Strbase <: Collection<Byte> & Equatable<String> & Comparable<String> & Hashable & T & String {}

extend String <: Strbase {}

main(): Int64 {
    0
}

/* SCAN
error: undeclared type name 'T'
 ==> extend_invalid_type02.cj:9:100:
  | 
9 | public interface Strbase <: Collection<Byte> & Equatable<String> & Comparable<String> & Hashable & T & String {}
  |                                                                                                    ^ 
  | 

error: interface 'Strbase' can only inherit interface
 ==> extend_invalid_type02.cj:9:1:
  | 
9 | public interface Strbase <: Collection<Byte> & Equatable<String> & Comparable<String> & Hashable & T & String {}
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
