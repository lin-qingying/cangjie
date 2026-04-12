// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

struct a func ( {
    let b : a var c : ((Unit * UInt32 * Range) * Bool * String * Int8 *
                    (Unit * UInt32 * Range) * Range) *
                   UInt32 * Option
                     match (c                       case (d,e : ,f=>                      a                 case k =>                      b                 case ((g : ,false,h,9,((),7,f  ),i  ),j,f=>                      a
