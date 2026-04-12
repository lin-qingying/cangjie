// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

public class TestThis {
    public func a(): This {
        this
    }
    public func c(): This {
        a().a()
    }
}

main() {}
/* SCAN
error: mismatched types
  ==> thisfuncchain.cj:13:27:
   | 
13 |       public func c(): This {
   |  ______________________~~~~_^
   | |                      |
   | |                      expected 'This' because of return type
14 | |         a().a()
15 | |     }
   | |_____^ expected 'This', found 'Class-TestThis'
   | 

1 error generated, 1 error printed.
*/
