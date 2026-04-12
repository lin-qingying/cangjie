// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

// random seed:1635836677524

// All global variable need to be defined before use!

main(): Int64 {
    return 0;
}

public open class Class8 {
    public static let var617: Option<Range<Int16>> = (match (((UInt16(221))..(UInt16(220)) : ((-61) | 1))) {
        case newVar: Range<Int16> => Option<Range<Int16>>.Some(newVar)
        case _ => Option<Range<Int16>>.None
    });
}
