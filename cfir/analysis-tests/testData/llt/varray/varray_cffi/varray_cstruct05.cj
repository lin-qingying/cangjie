// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC-PIPE-0: %run %run_opt %output %run_args

@C
struct S1 {
    var a: VArray<Int8, $3> = [11, 22, 33]
}

@C
struct S2 {
    var a: VArray<Int16, $3> = [11, 22, 33]
}

@C
struct S3 {
    var a: VArray<Int32, $3> = [11, 22, 33]
}

@C
struct S4 {
    var a: VArray<Int64, $3> = [11, 22, 33]
}

@C
struct S5 {
    var a: VArray<Float32, $3> = [1.1, 2.2, 3.3]
}

@C
struct S6 {
    var a: VArray<Float64, $3> = [1.1, 2.2, 3.3]
}

@C
struct S7 {
    var a: VArray<IntNative, $3> = [11, 22, 33]
}

@C
struct S8 {
    var a: VArray<CPointer<Unit>, $3> = VArray<CPointer<Unit>, $3>(repeat: CPointer<Unit>())
}

main() {
    unsafe {
        if (sizeOf<S1>() != sizeOf<Int8>() * 3) {
            return 1;
        }
        if (sizeOf<S2>() != sizeOf<Int16>() * 3) {
            return 2;
        }
        if (sizeOf<S3>() != sizeOf<Int32>() * 3) {
            return 3;
        }
        if (sizeOf<S4>() != sizeOf<Int64>() * 3) {
            return 4;
        }
        if (sizeOf<S5>() != sizeOf<Float32>() * 3) {
            return 5;
        }
        if (sizeOf<S6>() != sizeOf<Float64>() * 3) {
            return 6;
        }
        if (sizeOf<S7>() != sizeOf<IntNative>() * 3) {
            return 7;
        }
        if (sizeOf<S8>() != sizeOf<CPointer<Unit>>() * 3) {
            return 8;
        }
    }
    return 0;
}
