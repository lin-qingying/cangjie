// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// test ParseVArrayType.
main() {
    var a: VArray<!parse_varray_type_parameter!> <!>// error
    var a: VArray< // error
    <!parse_expected_type!>var<!> a: VArray<!parse_varray_type_args_mismatch!><Int64 // error
    (<!>)
    var a: VArray<Int64, // error
    <!parse_varray_type_args_mismatch!>(<!>)
    var a: VArray<Int64, <!parse_expect_integer_literal_varray!>$<!> // error
    var a: VArray<Int64, $1<!parse_expected_right_delimiter!> <!>// error
    var a: VArray<Int64, $1> // ok

    var a: VArray
    <Int64, $1> // ok
    var a: VArray<
    Int64, $1> // ok
    var a: VArray<Int64
    , $1> // ok
    var a: VArray<Int64,
    $1> // ok
    var a: VArray<Int64, <!parse_expect_integer_literal_varray!>$<!>
    1> // error, cannot wrap line between '$' and literal.
    <!parse_expected_expression!>var<!> a: VArray<Int64, $1
    > // ok
    0
}
