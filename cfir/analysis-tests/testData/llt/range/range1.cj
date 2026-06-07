// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


var gRange1 = 1..10:2
var gRange2 = <!parse_expected_expression!>..<!>10
var gRange3 = <!parse_expected_expression!>..<!><!parse_expected_expression!>:<!>3
var gRange4 = <!parse_expected_expression!>..<!>

<!parse_expected_expression!>main<!>() {
    var range1 = 1..10:2
    var range2 = <!parse_expected_expression!>..<!>10
    var range3 = <!parse_expected_expression!>..<!><!parse_expected_expression!>:<!>Int64(3)

    for (i in range1) {
        i
    }

    for <!parse_expected_left_paren!>i<!> in 1..20{}

    var arr = {<!parse_expected_double_arrow_in_lambda!>1<!><!parse_expected_character!>,<!>2,3,4,5<!parse_expected_expr_or_decl_in!>}<!>
    var arr1 = arr[1..3]
    var arr2 = arr[..]
    var arr3 = arr[range3]
    return 0
}
