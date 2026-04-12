// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main():Int64{
    var score: Int64 = 90
    var result: String = match score { 
        case 0 => "fail" 
        case 60 => "pass"  
        case 80 => "good"  
        case 90 => "excellent"  
        case _ => ""  
    }

    if (result == "excellent") {
        return 0
    }
    return 1

}

/* SCAN
error: expected '(' or '{' after 'match', found 'score'
  ==> match018.cj:11:32:
   | 
11 |     var result: String = match score { 
   |                          ~~~~~ ^^^^^ expected '(' or '{' here
   | 

1 error generated, 1 error printed.
*/
