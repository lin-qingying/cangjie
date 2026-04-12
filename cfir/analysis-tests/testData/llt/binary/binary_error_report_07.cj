// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

public let var_1691786195655_30: UInt64 = UInt64(1)
let var_1691786195655_37: UInt32 = UInt32(113)
public var var_1691786195655_69: Int64 = Int64(-110)
const var_1691786195655_108: Int = -47

main(): Int64 {
    var var_1691786195655_23: ?Int32 = Option<Int32>.Some(Int32(-72));
    const var_1691786195655_34: UInt16 = 153;
    (match (Int32(-29)) {
        case _ =>
            ();
            let var_1691786195655_17: Int = Int64(-17);
            var var_1691786195655_20: UInt = UInt64(37);
            var_1691786195655_17 ** var_0001022750302_20;
            var_1691786195655_23
    } ?? (var_1691786195655_30, (), var_1691786195655_34, var_1691786195655_37, Int32(-36), Int64(-58))[4]);
    1
}

/* SCAN
error: undeclared identifier 'var_0001022750302_20'
  ==> binary_error_report_07.cj:22:37:
   | 
22 |             var_1691786195655_17 ** var_0001022750302_20;
   |                                     ^ 
   | 

1 error generated, 1 error printed.
*/
