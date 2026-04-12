// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(_: VArray<Byte, $3>) {}

main() {
    f("123".toArray())
}

/* SCAN
error: mismatched types
  ==> err_byte_string.cj:12:7:
   | 
12 |     f("123".toArray())
   |       ^^^^^^^^^^^^^^^ expected 'VArray<UInt8, $3>', found 'Struct-Array<UInt8>'
   | 

1 error generated, 1 error printed.
*/
