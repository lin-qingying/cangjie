// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

var a = b()
let c = true
var d = e

enum f {
    e
}

class b {
    public let g: Int8 = 5
}

func h(i!: Int8 = match {
        case c => match (d) {
            case j: Float64 => a
        }
        case _ => a
    }.g) {}

/* SCAN
warning: unreachable pattern
  ==> type09.cj:23:18:
   | 
23 |             case j: Float64 => a
   |                  ^^^^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

error: non-exhaustive patterns
  ==> type09.cj:22:26:
   | 
22 |         case c => match (d) {
   |                          ^ the selector is of type 'Enum-f'
   | 
   # note: _ is not covered

error: 'main' is missing
 ==> type09.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
1 warning generated, 1 warning printed.
*/
