// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
// EXEC-PIPE-1: %compiler %f %cmp_opt -o %output 2>&1 | compare %f
main(){
    let a = Some(3)
    if(!(let Some(e) <- a)){
        print("test")
    }
    return 0
}
/* SCAN
error: expected expression after '(', found keyword 'let'
 ==> not.cj:9:10:
  | 
9 |     if(!(let Some(e) <- a)){
  |          ^^^ expected expression here
  | 

1 error generated, 1 error printed.
*/