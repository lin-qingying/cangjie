// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

public var var42: Int16 = Int16(38);
public var var272: Float32 = Float32(86.0);

struct Record14 {
    public func function296(): Float32 { // block id: 297

        let var316: Int16 = Int16(-45);

        match {
            case (var316 > { // block id: 333

                =>
                let var340: Record288 = Record288();

                var340;

                var42;
            }) => { // block id: 344
                => var316;
            }
            case _ => { // block id: 363

                => (var42 << (var316 & 7));
            }
        };
        var272
    }
}

main(): Int64 {
    return 0;
}

/* SCAN
error: undeclared type name 'Record288'
  ==> match_no_selector_fuzz_001.cj:21:29:
   | 
21 |                 let var340: Record288 = Record288();
   |                             ^ 
   | 

error: undeclared identifier 'Record288'
  ==> match_no_selector_fuzz_001.cj:21:41:
   | 
21 |                 let var340: Record288 = Record288();
   |                                         ^ 
   | 

2 errors generated, 2 errors printed.
*/
