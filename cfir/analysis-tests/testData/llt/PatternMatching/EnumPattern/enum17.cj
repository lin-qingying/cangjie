// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

enum a< b, c, d {
    e

enum f< b,  g {
    e
    | h(a, (Range, Range, (Range, (Int32, Range, (UInt8, Float64, Range), Range)), Option, UInt16, UInt32), Float32
}

func ({
    match (Option<f<UInt64, UInt8>>?? f.e
        case h(e, (i, j, (i, (i, k, (i, l, m), i)), None, n, o), p =>
        case q =>
        case h(e, (i, r, (s, (t, u, (i, v, i), i)), i, w, i), i =>
