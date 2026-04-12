// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func atest<T>(param: T) where T <: Array<T> {
    return param
}

/* SCAN
error: generic parameter 'T' cannot be used in class irrelevant upper bounds 'Struct-Array<Generics-T>'
 ==> generic_constraint9.cj:9:12:
  | 
9 | func atest<T>(param: T) where T <: Array<T> {
  |            ^ 
  | 

error: 'main' is missing
 ==> generic_constraint9.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
