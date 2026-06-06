// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

enum a< b, c, d<!parse_expected_right_delimiter!> <!>{
    e

<!parse_unexpected_declaration_in_scope!>enum<!> f< b,  g<!parse_expected_right_delimiter!> <!>{
    e
    | h(a, (Range, Range, (Range, (Int32, Range, (UInt8, Float64, Range), Range)), Option, UInt16, UInt32), Float32<!parse_expected_right_delimiter!>
<!>}

func <!parse_expected_name!>(<!><!parse_expected_name!>{<!>
    match (Option<f<UInt64, UInt8>>?? f.e<!parse_expected_right_delimiter!>
<!>        <!parse_expected_left_brace!>case<!> h(e, (i, j, (i, (i, k, (i, l, m), i)), None, n, o), p<!parse_expected_right_delimiter!> <!>=><!parse_case_body_cannot_be_empty!>
<!>        case q =><!parse_case_body_cannot_be_empty!>
<!>        case h(e, (i, r, (s, (t, u, (i, v, i), i)), i, w, i), i<!parse_expected_right_delimiter!> <!>=><!parse_case_body_cannot_be_empty!>
<!>
