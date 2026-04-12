// (CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f


func test<T>(a : T) : Bool where T <: Array<Bool> {
    if (a.isEmpty()) {
        return false
    }
    return a[0]
}

main() {
    if (!test([true])) {
        return 1
    }
    return 0
}

/* SCAN
error: the upper bound 'Struct-Array<Bool>' of generic parameter 'Generics-T' must be class or interface
 ==> array_access3.cj:4:11:
  | 
4 | func test<T>(a : T) : Bool where T <: Array<Bool> {
  |           ^ 
  | 

1 error generated, 1 error printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
