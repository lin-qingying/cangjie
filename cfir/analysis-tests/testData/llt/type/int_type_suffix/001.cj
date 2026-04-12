// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
internal import std.math.*

main() {
    // zeros
    let z01 = 0u8
    let t01 : UInt8 = 0
    if (z01 != t01) {
        return 1
    }
    let z02 = 0u16
    let t02 : UInt16 = 0
    if (z02 != t02) {
        return 1
    }
    let z03 = 0u32
    let t03 : UInt32 = 0
    if (z03 != t03) {
        return 1
    }
    let z04 = 0u64
    let t04 : UInt64 = 0
    if (z04 != t04) {
        return 1
    }
    let z05 = 0i8
    let t05 : Int8 = 0
    if (z05 != t05) {
        return 1
    }
    let z06 = 0i16
    let t06 : Int16 = 0
    if (z01 != t01) {
        return 1
    }
    let z07 = 0i32
    let t07 : Int32 = 0
    if (z07 != t07) {
        return 1
    }
    let z08 = 0i64
    let t08 : Int64 = 0
    if (z08 != t08) {
        return 1
    }

    let z09 = 0f16
    let t09 : Float16 = 0.0
    if (z09 != t09) {
        return 1
    }
    let z10 = 0f32
    let t10 : Float32 = 0.0
    if (z10 != t10) {
        return 1
    }
    let z11 = 0_f64
    let t11 : Float64 = 0.0
    if (z11 != t11) {
        return 1
    }
    let z12 = 0
    let t12 = 0
    if (z12 != t12) {
        return 1
    }
    let z13 = 0.0
    let t13  : Float64 = 0.0
    if (z13 != t13) {
        return 1
    }
    let z14 = 0.0f16
    let t14 : Float16 = 0.0
    if (z14 != t14) {
        return 1
    }
    let z15 = 0.0f32
    let t15 : Float32 = 0.0
    if (z15 != t15) {
        return 1
    }
    let z16 = 0.0f64
    let t16 : Float64 = 0.0
    if (z16 != t16) {
        return 1
    }
    let z17 = 0b0u8
    let t17 : UInt8 = 0b0
    if (z17 != t17) {
        return 1
    }
    let z18 = 0b0u16
    let t18 : UInt16 = 0b0
    if (z18 != t18) {
        return 1
    }
    let z19 = 0b0u32
    let t19 : UInt32 = 0b0
    if (z19 != t19) {
        return 1
    }
    let z20 = 0b0u64
    let t20 : UInt64 = 0b0
    if (z20 != t20) {
        return 1
    }

    let z21 = 0.0e6f64
    let z22 = 0.0e-6f64
    let z23 = 0.0_e-6_f64
    let z25 = 0.0_f64
    let z26 = 0_f64
    let z27 = .0f64
    let z28 = .0e10f64

    // ones
    let o01 = 1u8
    let p01 : UInt8 = 1
    if (p01 != o01) {
        return 1
    }
    let o02 = 1u16
    let p02 : UInt16 = 1
    if (p02 != o02) {
        return 1
    }
    let o03 = 1u32
    let p03 : UInt32 = 1
    if (p03 != o03) {
        return 1
    }
    let o04 = 1u64
    let p04 : UInt64 = 1
    if (p04 != o04) {
        return 1
    }
    let o05 = 1i8
    let p05 : Int8 = 1
    if (p05 != o05) {
        return 1
    }
    let o06 = 1i16
    let p06 : Int16 = 1
    if (p06 != o06) {
        return 1
    }
    let o07 = 1i32
    let p07 : Int32 = 1
    if (p07 != o07) {
        return 1
    }
    let o08 = 1i64
    let p08 : Int64 = 1
    if (p08 != o08) {
        return 1
    }
    let o09 = 1f16
    let p09 : Float16 = 1.0
    if (p09 != o09) {
        return 1
    }
    let o10 = 1f32
    let p10 : Float32 = 1.0
    if (p10 != o10) {
        return 1
    }
    let o11 = 1f64
    let p11 : Float64 = 1.0
    if (p11 != o11) {
        return 1
    }
    let o12 = 1
    let p12 : Int64 = 1
    if (p12 != o12) {
        return 1
    }
    let o13 = 1.0
    let p13 : Float64 = 1.0
    if (p13 != o13) {
        return 1
    }
    let o14 = 1.0f16
    let p14 : Float16 = 1.0
    if (p14 != o14) {
        return 1
    }
    let o15 = 1.0f32
    let p15 : Float32 = 1.0
    if (p15 != o15) {
        return 1
    }
    let o16 = 1.0f64
    let p16 : Float64 = 1.0
    if (p16 != o16) {
        return 1
    }
    let o17 = 0b1u8
    let p17 : UInt8 = 0b1
    if (p16 != o16) {
        return 1
    }
    let o18 = 0b1u16
    let p18 : UInt16 = 0b1
    if (p16 != o16) {
        return 1
    }
    let o19 = 0b1u32
    let p19 : UInt32 = 0b1
    if (p16 != o16) {
        return 1
    }
    let o20 = 0b1u64
    let p20 : UInt64 = 0b1
    if (p16 != o16) {
        return 1
    }
    let o21 = 1.2_f64
    let p21 : Float64 = 1.2
    if (p21 != o21) {
        return 1
    }
    let o22 = 0x5_u64
    let p22 : UInt64 = 0x5
    if (p22 != o22) {
        return 1
    }
    let o23 = 0o34523i32
    let p23 : Int32 = 0o34523i32
    if (p23 != o23) {
        return 1
    }

    // other random numbers
    let a0 = 0x123.abcdef15p1
    let a1 = 13432u32
    if (a1 != 13432) {
        return 1
    }
    let a2 = 432.4324e8f64
    if (a2 != 432.4324e8) {
        return 1
    }
    let a3 = 5434.432_321_246_654_e2_f64
    if (a3 != 5434.432_321_246_654_e2) {
        return 1
    }
    let a4 = 321_232_765_u32
    if (a4 != 321_232_765) {
        return 1
    }
    // minmax
    let axu6416max = 0xFFFF_FFFF_FFFF_FFFFu64
    if (axu6416max != UInt64.Max) {
        return 1
    }
    let adu6410max = 18446744073709551615u64
    if (adu6410max != UInt64.Max) {
        return 1
    }
    let axi6416max = 0x7FFF_FFFF_FFFF_FFFFi64
    if (axi6416max != Int64.Max) {
        return 1
    }
    let adi6410max = 9223372036854775807i64
    if (adi6410max != Int64.Max) {
        return 1
    }
    let axi6416min = -0x8000_0000_0000_0000
    if (axi6416min != Int64.Min) {
        return 1
    }
    let adi6410min = -9223372036854775808i64
    if (adi6410min != Int64.Min) {
        return 1
    }
    return 0;
}

/* SCAN
error: unknown suffix 'f16' for number literal
  ==> 001.cj:53:16:
   | 
53 |     let z09 = 0f16
   |                ^^^ unknown suffix 'f16'
   | 

error: unknown suffix 'f32' for number literal
  ==> 001.cj:58:16:
   | 
58 |     let z10 = 0f32
   |                ^^^ unknown suffix 'f32'
   | 

error: unknown suffix 'f64' for number literal
  ==> 001.cj:63:17:
   | 
63 |     let z11 = 0_f64
   |                 ^^^ unknown suffix 'f64'
   | 

error: unknown suffix 'f64' for number literal
   ==> 001.cj:118:17:
    | 
118 |     let z26 = 0_f64
    |                 ^^^ unknown suffix 'f64'
    | 

error: unknown suffix 'f16' for number literal
   ==> 001.cj:163:16:
    | 
163 |     let o09 = 1f16
    |                ^^^ unknown suffix 'f16'
    | 

error: unknown suffix 'f32' for number literal
   ==> 001.cj:168:16:
    | 
168 |     let o10 = 1f32
    |                ^^^ unknown suffix 'f32'
    | 

error: unknown suffix 'f64' for number literal
   ==> 001.cj:173:16:
    | 
173 |     let o11 = 1f64
    |                ^^^ unknown suffix 'f64'
    | 

7 errors generated, 7 errors printed.
*/
