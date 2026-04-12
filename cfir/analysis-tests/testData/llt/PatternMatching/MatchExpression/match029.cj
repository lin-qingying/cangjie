// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %n.cj -o %output

// scan: 1 error generated, 1 error printed.

main() {
    let x = 0
    match (x) {
        case 0 => 0
        case _ => match (x) {
            case 0 => 1
            case _ => "a"
        }
    }
}

/* SCAN
error: type incompatible in this MatchCase
  ==> match029.cj:15:19:
   | 
15 |         case _ => match (x) {
   |                   ^ 
   | 
   # note: Traces:
The types 'Int64' and 'Struct-String' do not have the smallest common supertype


1 error generated, 1 error printed.
*/
