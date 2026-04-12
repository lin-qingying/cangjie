// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./f2 ./g1
// EXEC: %compiler %cmp_opt %compile_lib_opt -Woff all %overflowOpt -p ./f2 -o f2.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt -Woff all %overflowOpt -p ./g1 -o g1.%middle
// EXEC: %compiler %cmp_opt f2.%middle g1.%middle %overflowOpt -p ./ -o %output
// EXEC: %run %run_opt %output

// random seed:1731854046694
// All global variable need to be defined before use!

public import default.g1.*

const var_1731854046694_51: Unit = ()
const var_1731854046694_91: UInt16 = 62
public var var_1731854046694_2160: Array<Range<Int8>> = [((Int8(Int8(14)) & 127)..(Int8(Int8(-109)) & 127) : (Int64(57) |
    1)), ((Int8(Int8(-96)) & 127)..=(Int8(Int8(-70)) & 127) : (Int64(112) | 1)),
    ((Int8(Int8(57)) & 127)..(Int8(Int8(-45)) & 127) : (Int64(-68) | 1)),
    ((Int8(Int8(24)) & 127)..=(Int8(Int8(21)) & 127) : (Int64(-50) | 1)),
    ((Int8(Int8(80)) & 127)..=(Int8(Int8(103)) & 127) : (Int64(-72) | 1)),
    ((Int8(Int8(-55)) & 127)..(Int8(Int8(62)) & 127) : (Int64(-45) | 1)),
    ((Int8(Int8(-43)) & 127)..=(Int8(Int8(-105)) & 127) : (Int64(25) | 1)),
    ((Int8(Int8(-36)) & 127)..=(Int8(Int8(12)) & 127) : (Int64(115) | 1)),
    ((Int8(Int8(-65)) & 127)..(Int8(Int8(-54)) & 127) : (Int64(-106) | 1)),
    ((Int8(Int8(32)) & 127)..(Int8(Int8(-49)) & 127) : (Int64(23) | 1))]
internal let var_1731854046694_2176: Array<Unit> = [(), (), (), (), (), (), (), (), (), ()]
@Deprecated
public var var_1731854046694_2218: Option<Unit> = Option<Unit>.None
public let var_1731854046694_3090: Range<UInt64> = ((UInt64(UInt64(62)) & 127)..=(UInt64(UInt64(213)) & 127) : (Int64(53) |
    1))
public const var_1731854046694_4157: Int32 = 37
var var_1731854046694_4314: (Float64, Float64, Range<Int32>, UInt32, UInt8) = (Float64(Float64(Float32(Float32(-127.0)))),
    Float64(27.0), ((Int32(Int32(-38)) & 127)..=(Int32(Int32(UInt8(UInt8(34)))) & 127) : (Int64(Int64(116)) | 1)),
    UInt32(UInt8(66)), UInt8(156))
let var_1731854046694_4527: ?Unit = Option<Unit>.None

public open class Class_1731854046694_118<T0, T1> <: Interface_1731854046694_121<Range<Int8>, UInt32> & Interface_1731854046694_131 {
    public init() { // block id: 120

    }

    public static func func_1731854046694_196(param_1731854046694_197: Range<UInt32>, param_1731854046694_198: Float64,
        namedParam_1731854046694_199!: Int8, namedParam_1731854046694_200!: Int64): Range<UInt64> { // block id: 3069
        match ([((UInt64(UInt64(174)) & 127)..(UInt64(UInt64(254)) & 127) : (Int64(-5) | 1)),
            ((UInt64(UInt64(185)) & 127)..=(UInt64(UInt64(50)) & 127) : (Int64(-32) | 1)),
            ((UInt64(UInt64(254)) & 127)..=(UInt64(UInt64(19)) & 127) : (Int64(78) | 1)),
            ((UInt64(UInt64(180)) & 127)..(UInt64(UInt64(169)) & 127) : (Int64(74) | 1)),
            ((UInt64(UInt64(139)) & 127)..(UInt64(UInt64(17)) & 127) : (Int64(-76) | 1)),
            ((UInt64(UInt64(199)) & 127)..=(UInt64(UInt64(150)) & 127) : (Int64(91) | 1)),
            ((UInt64(UInt64(148)) & 127)..(UInt64(UInt64(151)) & 127) : (Int64(-18) | 1)),
            ((UInt64(UInt64(17)) & 127)..(UInt64(UInt64(171)) & 127) : (Int64(79) | 1)),
            ((UInt64(UInt64(14)) & 127)..(UInt64(UInt64(54)) & 127) : (Int64(64) | 1)),
            ((UInt64(UInt64(247)) & 127)..(UInt64(UInt64(145)) & 127) : (Int64(45) | 1))]) {
            case _ => ()

            case caseVar_1731854046694_3078: Range<Int8> =>
                var var_1731854046694_3081: Unit = ();
                var_1731854046694_3081;
                ()

            case _ =>
                ();
                var_1731854046694_51
        };
        var_1731854046694_3090
    };

    public static var var_1731854046694_4280: UInt32 = UInt32(141);
    public static func func_1731854046694_359(): UInt16 { // block id: 4121
        match (if (var_1731854046694_2266) { // block id: 4125
            let var_1731854046694_4127: Array<Range<UInt64>> = [((UInt64(UInt64(189)) & 127)..(UInt64(UInt64(86)) & 127) : (Int64(27) |
                1)), ((UInt64(UInt64(150)) & 127)..=(UInt64(UInt64(48)) & 127) : (Int64(43) | 1)),
                ((UInt64(UInt64(235)) & 127)..(UInt64(UInt64(241)) & 127) : (Int64(-1) | 1)),
                ((UInt64(UInt64(214)) & 127)..(UInt64(UInt64(162)) & 127) : (Int64(-121) | 1)),
                ((UInt64(UInt64(58)) & 127)..(UInt64(UInt64(75)) & 127) : (Int64(79) | 1)),
                ((UInt64(UInt64(19)) & 127)..(UInt64(UInt64(116)) & 127) : (Int64(-81) | 1)),
                ((UInt64(UInt64(Int64(22))) & 127)..=(UInt64(UInt64(Int16(UInt8(57)))) & 127) : (Int64(UInt8(207)) | 1)),
                ((UInt64(UInt64(63)) & 127)..=(UInt64(UInt64(95)) & 127) : (Int64(25) | 1)),
                ((UInt64(UInt64(Int32(84))) & 127)..(UInt64(UInt64(Int16(Int16(12)))) & 127) : (Int64(-106) | 1)),
                ((UInt64(UInt64(94)) & 127)..(UInt64(UInt64(108)) & 127) : (Int64(-59) | 1))];
            var_1731854046694_4127;
            ();
            var_1731854046694_4157
        } else if ((Option<Bool>.Some(var_1731854046694_2266) ?? false)) { // block id: 4167
            var_1731854046694_51;
            var_1731854046694_4157
        } else { // block id: 4170
            ();
            var_1731854046694_4157 - var_1731854046694_4157
        }) {
            case caseVar_1731854046694_4177 =>
                var var_1731854046694_4180: (Option<Array<Range<Int8>>>, (Float64, Float64, Range<Int32>, UInt32, UInt8), 
                    (String, Unit, Unit, Array<Unit>, Range<UInt32>, Range<Int64>), UInt8, ((String, Unit, Unit, Array<Unit>, 
                    Range<UInt32>, Range<Int64>), Range<UInt64>, Option<Unit>), Array<Range<Int16>>, Range<Int64>, 
                    UInt8, Float64) = (Option<Array<Range<Int8>>>.None, (Float64(122.0), Float64(Float64(Float64(92.0))),
                        ((Int32(Int32(-46)) & 127)..(Int32(Int32(-54)) & 127) : (Int64(69) | 1)), UInt32(Int32(117)),
                        UInt8(UInt8(117))), (###"Z5fQRNc\"Hm"###, (), (), [(), (), (), (), (), (), (), (), (), ()], ((UInt32(UInt32(222)) &
                            127)..(UInt32(UInt32(94)) & 127) : (Int64(UInt16(15)) | 1)), ((Int64(Int64(-64)) & 127)..(Int64(Int64(-30)) &
                            127) : (Int64(34) | 1))), UInt8(227), (("""
#fmH1
pSxlz
""", (), (), [(), (), (), (), (), (),
                                (), (), (), ()], ((UInt32(UInt32(191)) & 127)..=(UInt32(UInt32(60)) & 127) : (Int64(76) |
                                1)), ((Int64(Int64(8)) & 127)..(Int64(Int64(-102)) & 127) : (Int64(-118) | 1))), ((UInt64(UInt64(118)) &
                            127)..=(UInt64(UInt64(138)) & 127) : (Int64(115) | 1)), (UInt32(56) as Unit)), [((Int16(Int16(123)) &
                        127)..(Int16(Int16(73)) & 127) : (Int64(37) | 1)),
                        ((Int16(Int16(-72)) & 127)..=(Int16(Int16(UInt16(122))) & 127) : (Int64(UInt8(45)) | 1)),
                        ((Int16(Int16(Int16(27))) & 127)..(Int16(Int16(Int16(-14))) & 127) : (Int64(Int64(-34)) | 1)),
                        ((Int16(Int16(0)) & 127)..(Int16(Int16(UInt8(195))) & 127) : (Int64(116) | 1)),
                        ((Int16(Int16(36)) & 127)..=(Int16(Int16(73)) & 127) : (Int64(50) | 1)),
                        ((Int16(Int16(104)) & 127)..=(Int16(Int16(-15)) & 127) : (Int64(36) | 1)),
                        ((Int16(Int16(-100)) & 127)..=(Int16(Int16(-69)) & 127) : (Int64(-101) | 1)),
                        ((Int16(Int16(60)) & 127)..(Int16(Int16(41)) & 127) : (Int64(78) | 1)),
                        ((Int16(Int16(-33)) & 127)..=(Int16(Int16(101)) & 127) : (Int64(115) | 1)),
                        ((Int16(Int16(Int16(83))) & 127)..(Int16(Int16(-74)) & 127) : (Int64(Int64(-121)) | 1))], ((Int64(Int64(Int64(-2))) &
                        127)..=(Int64(Int64(UInt32(Int32(109)))) & 127) : (Int64(-97) | 1)), UInt8(UInt8(Int8(89))),
                    Float64(40.0));
                var_1731854046694_4180;
                var_1731854046694_51

            case _ => var_1731854046694_4280 += UInt32(122)
        };
        var_1731854046694_111;
        const var_1731854046694_4304: UInt8 = 168;
        const var_1731854046694_4309: Bool = true;
        if (([Float64(64.0), Float64(-125.0), Float64(-58.0), Float64(28.0), Float64(-81.0), Float64(-51.0),
            Float64(-53.0), Float64(100.0), Float64(77.0), Float64(88.0)] is Int8)) { // block id: 4287
            let var_1731854046694_4294: ?Int32 = (() as Int32);
            (var_1731854046694_4294 ?? var_1731854046694_4157);
            if (true) { // block id: 4300
                UInt8(119) | var_1731854046694_4304;
                -UInt16(72)
            } else if (var_1731854046694_4309) { // block id: 4311
                var_1731854046694_51;
                var_1731854046694_4314;
                UInt16(102)
            } else { // block id: 4332
                UInt16(221) & var_1731854046694_91
            }
        } else if (try { // block id: 4337
            !true
        } catch (_) { // block id: 4340
            ((Int32(Int32(-88)) & 127)..=(Int32(Int32(29)) & 127) : (Int64(-117) | 1));
            !false
        }) { // block id: 4344
            var var_1731854046694_4346: Int8 = Int8(Int8(-96));
            var_1731854046694_4346;
            if (var_1731854046694_4309) { // block id: 4351
                var_1731854046694_91 ^ UInt16(103)
            } else { // block id: 4355
                var_1731854046694_51;
                var_1731854046694_91 | UInt16(72)
            }
        } else { // block id: 4360
            Int16(72) - var_1731854046694_111;
            let var_1731854046694_4371: Bool = ((Float64(Float64(4.0)), Float64(Float64(Float32(35.0))), ((Int32(Int32(125)) &
                    127)..=(Int32(Int32(-118)) & 127) : (Int64(92) | 1)), UInt32(Int8(67)), UInt8(191)) is ((String, 
                Unit, Unit, Array<Unit>, Range<UInt32>, Range<Int64>), Range<UInt64>, Option<Unit>));
            if (var_1731854046694_4309) { // block id: 4366
                var_1731854046694_4304 ^ UInt8(24)
            } else if (var_1731854046694_4371) { // block id: 4383
                UInt8(92) << ((UInt8(188)) & 7)
            } else { // block id: 4387
                var var_1731854046694_4390: UInt16 = UInt16(UInt16(99));
                var_1731854046694_4390 |= UInt16(31)
                var var_1731854046694_4397: Array<Bool> = [false, false, true, false, true, false, false, true, true,
                    true];
                if (true) {
                    var_1731854046694_4397
                } else {
                    [true, false, var_1731854046694_4371, var_1731854046694_4309, true, var_1731854046694_4309,
                        var_1731854046694_4309, var_1731854046694_2266, var_1731854046694_4309, var_1731854046694_2266]
                };
                UInt8(187)
            };
            UInt16(145) - UInt16(159)
        }
    };

    public static func func_1731854046694_189(namedParam_1731854046694_190!: Range<Int16>,
        namedParam_1731854046694_191!: Range<Int64>, namedParam_1731854046694_192!: Range<UInt16>,
        namedParam_1731854046694_193!: UInt64): Unit { // block id: 4498

    }

    mut static prop var_1731854046694_4504: Bool { // block id: 4505
        get() { // block id: 4507
            Int8(-126);
            ();
            (([((Int8(Int8(59)) & 127)..(Int8(Int8(4)) & 127) : (Int64(-111) | 1)),
                ((Int8(Int8(24)) & 127)..(Int8(Int8(-102)) & 127) : (Int64(-72) | 1)),
                ((Int8(Int8(-92)) & 127)..(Int8(Int8(-114)) & 127) : (Int64(-36) | 1)),
                ((Int8(Int8(73)) & 127)..=(Int8(Int8(100)) & 127) : (Int64(-12) | 1)),
                ((Int8(Int8(-12)) & 127)..=(Int8(Int8(-58)) & 127) : (Int64(19) | 1)),
                ((Int8(Int8(-69)) & 127)..=(Int8(Int8(109)) & 127) : (Int64(-116) | 1)),
                ((Int8(Int8(-8)) & 127)..=(Int8(Int8(-111)) & 127) : (Int64(-116) | 1)),
                ((Int8(Int8(-68)) & 127)..(Int8(Int8(-16)) & 127) : (Int64(-86) | 1)),
                ((Int8(Int8(58)) & 127)..(Int8(Int8(-85)) & 127) : (Int64(65) | 1)),
                ((Int8(Int8(-32)) & 127)..(Int8(Int8(61)) & 127) : (Int64(54) | 1))] as Int32) is Array<Unit>)
        }
        set(Value) { // block id: 4515
            ()
        }
    }
    var var_1731854046694_4556: Int16 = Int16(93);
    mut prop var_1731854046694_4632: ?Unit { // block id: 4633
        get() { // block id: 4635
            Int8(-14);
            Option<Unit>.None
        }
        set(Value) { // block id: 4640
            UInt64(45);
            ((UInt8(UInt8(181)) & 127)..=(UInt8(UInt8(247)) & 127) : (Int64(-113) | 1));
            ()
        }
    }
    public open override func func_1731854046694_2275(param_1731854046694_2276: Array<Range<UInt64>>): Int64 { // block id: 4500
        Option<Bool>.None;
        if (var_1731854046694_4504) { // block id: 4517
            (Option<Unit>.None ?? ());
            (var_1731854046694_4527 ?? ())
        };
        var var_1731854046694_4584: (String, Unit, Unit, Array<Unit>, Range<UInt32>, Range<Int64>) = ("""
9Q}^W
Wl8\"r
""",
            (), (), [(), (), (), (), (), (), (), (), (), ()], ((UInt32(UInt32(0)) & 127)..=(UInt32(UInt32(Int8(UInt8(9)))) &
                127) : (Int64(UInt64(128)) | 1)), ((Int64(Int64(-105)) & 127)..(Int64(Int64(Int64(UInt64(22)))) & 127) : (Int64(-83) |
                1)));
        let var_1731854046694_4621: Bool = true;
        if ((Option<Bool>.Some(var_1731854046694_4538) ?? try { // block id: 4542
            Option<Unit>.None;
            var_1731854046694_51;
            false && var_1731854046694_4504
        } catch (_) { // block id: 4548
            var_1731854046694_2266
        })) { // block id: 4550
            var loopTimeVar_1731854046694_4560: Int64 = 0;
            do {
                loopTimeVar_1731854046694_4560++;
                var_1731854046694_4556 &= var_1731854046694_4556
            } while ((var_1731854046694_4504) && loopTimeVar_1731854046694_4560 < 100);
            const var_1731854046694_4566: String = ###"\\Za#LEnHT,"###;
            let var_1731854046694_4571: Array<Unit> = [(), (), (), (), (), (), (), (), (), ()];
            try { // block id: 4562
                var_1731854046694_51;
                (var_1731854046694_4566, var_1731854046694_51, var_1731854046694_51, var_1731854046694_4571, ((UInt32(var_1731854046694_4280) &
                        127)..=(UInt32(UInt32(238)) & 127) : (Int64(5) | 1)), ((Int64(Int64(-107)) & 127)..(Int64(var_1731854046694_1994) &
                        127) : (var_1731854046694_1994 | 1)))
            } finally { // block id: 4581
                var_1731854046694_51;
                var_1731854046694_4584
            };
            try { // block id: 4614
                Int64(65) - Int64(-119)
            } finally { // block id: 4618
                if (var_1731854046694_4621) {
                    ((UInt32(UInt32(23)) & 127)..(UInt32(UInt32(219)) & 127) : (var_1731854046694_1994 | 1))
                } else {
                    ((UInt32(UInt32(209)) & 127)..=(UInt32(UInt32(32)) & 127) : (Int64(92) | 1))
                };
                var_1731854046694_1994
            }
        } else { // block id: 4629
            (var_1731854046694_4632 ?? var_1731854046694_51);
            var var_1731854046694_4650: UInt64 = UInt64(0);
            if (false) { // block id: 4647
                var_1731854046694_4650 <<= ((UInt64(141)) & 7)
                var_1731854046694_51
            } else if (var_1731854046694_4538) { // block id: 4655
                ();
                var_1731854046694_51
            };
            try { // block id: 4659
                var_1731854046694_51;
                var_1731854046694_51;
                Int64(63) ^ var_1731854046694_1994
            } catch (_) { // block id: 4665
                Int64(-29)
            }
        }
    }
}

main(): Int64 {
    try {
        var var_1731854046694_22: Range<UInt8> = ((UInt8(UInt8(80)) & 127)..=(UInt8(UInt8(UInt8(UInt8(Int8(-26))))) & 127) : (Int64(-33) |
            1));
        match (if (false) {
            ((UInt8(UInt8(38)) & 127)..(UInt8(UInt8(44)) & 127) : (Int64(-63) | 1))
        } else {
            var_1731854046694_22
        }) {
            case _ =>
                let var_1731854046694_34: Unit = ();
                var_1731854046694_34;
                [var_1731854046694_34, var_1731854046694_34, var_1731854046694_34, (), var_1731854046694_34, (), (),
                    var_1731854046694_34, (), ()]

            case _ =>
                var_1731854046694_51;
                let var_1731854046694_55: Bool = false;
                if (var_1731854046694_55) {
                    [var_1731854046694_51, (), (), (), (), var_1731854046694_51, var_1731854046694_51, (), (), ()]
                } else {
                    [(), var_1731854046694_51, (), (), (), (), var_1731854046694_51, (), var_1731854046694_51, ()]
                }
        }[8];
        var var_1731854046694_86: Bool = (Int64(36) is Array<UInt64>);
        const var_1731854046694_102: Int16 = 66;
        var loopTimeVar_1731854046694_116: Int64 = 0;
        while ((try { // block id: 82
                false || var_1731854046694_86
            } finally { // block id: 89
                var_1731854046694_91;
                false
            }) && loopTimeVar_1731854046694_116 < 100) {
            loopTimeVar_1731854046694_116++;
            ((UInt64(UInt64(145)) & 127)..(UInt64(UInt64(168)) & 127) : (Int64(119) | 1));
            let var_1731854046694_107: UInt8 = UInt8(144);
            try { // block id: 100
                var_1731854046694_102
            } finally { // block id: 104
                ((Int64(Int64(38)) & 127)..(Int64(Int64(41)) & 127) : (Int64(23) | 1));
                var_1731854046694_107;
                var_1731854046694_111 | Int16(49)
            }
        };
        const var_1731854046694_1977: UInt32 = 188;
        const var_1731854046694_1985: UInt8 = 184;
        let var_1731854046694_2015: Array<UInt64> = [UInt64(186), UInt64(222), UInt64(66), UInt64(106), UInt64(230),
            UInt64(210), UInt64(83), UInt64(47), UInt64(145), UInt64(248)];
        let var_1731854046694_2056: (Option<Array<Range<Int8>>>, (Float64, Float64, Range<Int32>, UInt32, UInt8), (String, 
            Unit, Unit, Array<Unit>, Range<UInt32>, Range<Int64>), UInt8, ((String, Unit, Unit, Array<Unit>, Range<UInt32>, 
            Range<Int64>), Range<UInt64>, Option<Unit>), Array<Range<Int16>>, Range<Int64>, UInt8, Float64) = ((UInt64(49) as Array<Range<Int8>>),
            (Float64(Float64(Float64(114.0))), Float64(16.0), ((Int32(Int32(UInt32(26))) & 127)..=(Int32(Int32(-32)) &
                    127) : (Int64(81) | 1)), UInt32(Int8(Int8(-80))), UInt8(68)), (###"x1-&koREM!"###, (), (), [(), (),
                    (), (), (), (), (), (), (), ()], ((UInt32(UInt32(249)) & 127)..(UInt32(UInt32(Int16(-112))) & 127) : (Int64(Int8(-50)) |
                    1)), ((Int64(Int64(44)) & 127)..(Int64(Int64(46)) & 127) : (Int64(UInt32(135)) | 1))), UInt8(77), ((###" W*cNnUS38"###,
                    (), (), [(), (), (), (), (), (), (), (), (), ()], ((UInt32(UInt32(187)) & 127)..=(UInt32(UInt32(220)) &
                        127) : (Int64(60) | 1)), ((Int64(Int64(-117)) & 127)..=(Int64(Int64(11)) & 127) : (Int64(39) | 1))),
                ((UInt64(UInt64(215)) & 127)..(UInt64(UInt64(103)) & 127) : (Int64(118) | 1)), Option<Unit>.None), [((Int16(Int16(-98)) &
                127)..(Int16(Int16(59)) & 127) : (Int64(9) | 1)),
                ((Int16(Int16(-75)) & 127)..=(Int16(Int16(78)) & 127) : (Int64(36) | 1)),
                ((Int16(Int16(32)) & 127)..(Int16(Int16(46)) & 127) : (Int64(85) | 1)),
                ((Int16(Int16(-102)) & 127)..=(Int16(Int16(-76)) & 127) : (Int64(-63) | 1)),
                ((Int16(Int16(2)) & 127)..(Int16(Int16(62)) & 127) : (Int64(-88) | 1)),
                ((Int16(Int16(-4)) & 127)..(Int16(Int16(62)) & 127) : (Int64(-120) | 1)),
                ((Int16(Int16(116)) & 127)..(Int16(Int16(-78)) & 127) : (Int64(59) | 1)),
                ((Int16(Int16(17)) & 127)..=(Int16(Int16(-100)) & 127) : (Int64(112) | 1)),
                ((Int16(Int16(97)) & 127)..=(Int16(Int16(-61)) & 127) : (Int64(58) | 1)),
                ((Int16(Int16(-85)) & 127)..=(Int16(Int16(-96)) & 127) : (Int64(-39) | 1))], ((Int64(Int64(34)) & 127)..=(Int64(Int64(10)) &
                127) : (Int64(-51) | 1)), UInt8(122), Float64(108.0));
        let var_1731854046694_2189: Range<UInt32> = ((UInt32(UInt32(253)) & 127)..=(UInt32(UInt32(184)) & 127) : (Int64(94) |
            1));
        let var_1731854046694_2212: Range<UInt64> = ((UInt64(UInt64(56)) & 127)..(UInt64(UInt64(76)) & 127) : (Int64(-96) |
            1));
        func func_1731854046694_133(param_1731854046694_134: Range<Int32>): Class_1731854046694_118<Int32, Range<UInt32>> { // block id: 135
            var var_1731854046694_1949: Bool = ((((Int16(Int16(94)) & 127)..(Int16(Int16(Int16(54))) & 127) : (Int64(90) |
                    1)), UInt16(113), UInt32(UInt32(224)), ((Int8(Int8(-70)) & 127)..=(Int8(Int8(-70)) & 127) : (Int64(22) |
                    1)), ((UInt16(UInt16(111)) & 127)..=(UInt16(UInt16(65)) & 127) : (Int64(-94) | 1))) is Range<UInt16>);
            const var_1731854046694_1972: Int32 = 100;
            func_1731854046694_137(
                if (var_1731854046694_1949) {
                    (Float64(10.0), Float64(1.0), ((Int32(var_1731854046694_1972) & 127)..(Int32(Int32(-36)) & 127) : (Int64(-107) |
                            1)), var_1731854046694_1977, UInt8(33))[2]
                } else {
                    try { // block id: 1982
                        var_1731854046694_1985 >> ((UInt8(214)) & 7);
                        if (var_1731854046694_1949) {
                            ((Int32(Int32(-86)) & 127)..(Int32(Int32(-37)) & 127) : (var_1731854046694_1994 | 1))
                        } else {
                            ((Int32(Int32(14)) & 127)..=(Int32(Int32(71)) & 127) : (Int64(-60) | 1))
                        }
                    } finally { // block id: 1997
                        ((Int32(var_1731854046694_1972) & 127)..=(Int32(Int32(-5)) & 127) : (Int64(-70) | 1))
                    }
                },
                namedParam_1731854046694_139: match (UInt32(33)) {
                    case caseVar_1731854046694_2006: Range<UInt8> =>
                        var var_1731854046694_2010: UInt8 = UInt8(UInt8(Int8(-75)));
                        var_1731854046694_2010++;
                        var_1731854046694_2015;
                        let var_1731854046694_2018: Range<UInt64> = ((UInt64(UInt64(149)) & 127)..=(UInt64(UInt64(33)) &
                            127) : (Int64(63) | 1));
                        var_1731854046694_2018

                    case 34 =>
                        ((Int64(Int64(-61)) & 127)..(Int64(var_1731854046694_1994) & 127) : (var_1731854046694_1994 | 1));
                        var_1731854046694_51;
                        ((UInt64(var_1731854046694_2030) & 127)..=(UInt64(UInt64(13)) & 127) : (Int64(-98) | 1))

                    case _ =>
                        var var_1731854046694_2044: Range<UInt64> = ((UInt64(UInt64(150)) & 127)..(UInt64(UInt64(103)) &
                            127) : (Int64(50) | 1));
                        if (false) {
                            ((UInt64(var_1731854046694_2030) & 127)..=(UInt64(UInt64(31)) & 127) : (var_1731854046694_1994 |
                                1))
                        } else {
                            var_1731854046694_2044
                        }
                });
            var var_1731854046694_2221: Array<Range<Int16>> = [((Int16(Int16(-125)) & 127)..(Int16(Int16(20)) & 127) : (Int64(74) |
                1)), ((Int16(Int16(-22)) & 127)..(Int16(Int16(114)) & 127) : (Int64(-42) | 1)),
                ((Int16(Int16(2)) & 127)..=(Int16(Int16(93)) & 127) : (Int64(-124) | 1)),
                ((Int16(Int16(111)) & 127)..(Int16(Int16(26)) & 127) : (Int64(-17) | 1)),
                ((Int16(Int16(-68)) & 127)..(Int16(Int16(-123)) & 127) : (Int64(25) | 1)),
                ((Int16(Int16(-69)) & 127)..=(Int16(Int16(-100)) & 127) : (Int64(125) | 1)),
                ((Int16(Int16(16)) & 127)..=(Int16(Int16(97)) & 127) : (Int64(-49) | 1)),
                ((Int16(Int16(-10)) & 127)..=(Int16(Int16(112)) & 127) : (Int64(-55) | 1)),
                ((Int16(Int16(-34)) & 127)..=(Int16(Int16(13)) & 127) : (Int64(26) | 1)),
                ((Int16(Int16(-55)) & 127)..(Int16(Int16(37)) & 127) : (Int64(76) | 1))];
            var var_1731854046694_2224: Range<Int64> = ((Int64(Int64(-113)) & 127)..=(Int64(Int64(-126)) & 127) : (Int64(-69) |
                1));
            (var_1731854046694_2056[4][2] ?? match (try { // block id: 2125
                var_1731854046694_2056
            } catch (e: Exception) { // block id: 2127
                var_1731854046694_2130 /= Float32((Int32(Float32(-51.0))) | 1)
                ();
                var_1731854046694_2056
            } finally { // block id: 2138
                var var_1731854046694_2141: Range<Int16> = ((Int16(Int16(Int8(-1))) & 127)..(Int16(Int16(UInt8(178))) &
                    127) : (Int64(UInt8(UInt8(83))) | 1));
                var var_1731854046694_2154: Range<UInt16> = ((UInt16(UInt16(56)) & 127)..(UInt16(UInt16(129)) & 127) : (Int64(-38) |
                    1));
                (var_1731854046694_2141, var_1731854046694_91, UInt32(16), ((Int8(Int8(8)) & 127)..(Int8(Int8(-12)) & 127) : (Int64(23) |
                        1)), var_1731854046694_2154);
                ();
                (Option<Array<Range<Int8>>>.Some(var_1731854046694_2160), (Float64(-16.0), Float64(-48.0), ((Int32(var_1731854046694_1972) &
                            127)..(Int32(var_1731854046694_1972) & 127) : (var_1731854046694_1994 | 1)),
                        var_1731854046694_1977, UInt8(173)), ("""
D1nTZ
}I,\\f
""", (), (), var_1731854046694_2176,
                        var_1731854046694_2189, ((Int64(Int64(126)) & 127)..=(Int64(Int64(35)) & 127) : (Int64(-103) | 1))),
                    var_1731854046694_1985, (("""
Y)/|?
;nI'@
""", var_1731854046694_51, var_1731854046694_51, [var_1731854046694_51,
                                var_1731854046694_51, (), var_1731854046694_51, (), (), var_1731854046694_51,
                                var_1731854046694_51, (), ()], ((UInt32(UInt32(99)) & 127)..=(UInt32(UInt32(9)) & 127) : (Int64(20) |
                                1)), ((Int64(Int64(58)) & 127)..=(Int64(Int64(-49)) & 127) : (Int64(-54) | 1))),
                        var_1731854046694_2212, var_1731854046694_2218), var_1731854046694_2221, var_1731854046694_2224,
                    var_1731854046694_1985, Float64(81.0))
            }) {
                case (_: Rune, (caseVar_1731854046694_2230, caseVar_1731854046694_2231, _: Int8, caseVar_1731854046694_2232: Int32, _), _: Range<UInt64>, caseVar_1731854046694_2233, (caseVar_1731854046694_2234: Int8, caseVar_1731854046694_2235, caseVar_1731854046694_2236), _: (Option<Array<Range<Int8>>>, 
                            (Float64, Float64, Range<Int32>, UInt32, UInt8), (String, Unit, Unit, Array<Unit>, Range<UInt32>, 
                            Range<Int64>), UInt8, ((String, Unit, Unit, Array<Unit>, Range<UInt32>, Range<Int64>), Range<UInt64>, 
                            Option<Unit>), Array<Range<Int16>>, Range<Int64>, UInt8, Float64), _, _, caseVar_1731854046694_2237: Array<Bool>) =>
                    var_1731854046694_2224;
                    var_1731854046694_51

                case _ =>
                    var var_1731854046694_2245: Range<Int16> = ((Int16(Int16(-36)) & 127)..=(Int16(Int16(-110)) & 127) : (Int64(108) |
                        1));
                    var_1731854046694_2245;
                    ()
            });
            var var_1731854046694_2256: ?Class_1731854046694_118<Int32, Range<UInt32>> = Option<Class_1731854046694_118<Int32,
                    Range<UInt32>>>.None;
            let var_1731854046694_2262: Class_1731854046694_118<Int32, Range<UInt32>> = Class_1731854046694_118<Int32, Range<UInt32>>();
            try { // block id: 2249
                (var_1731854046694_2256 ?? Class_1731854046694_118<Int32, Range<UInt32>>())
            } finally { // block id: 2259
                Class_1731854046694_118<Int32, Range<UInt32>>();
                var_1731854046694_2262
            }
        };

        let var_1731854046694_2270: Range<Int32> = ((Int32(Int32(95)) & 127)..(Int32(Int32(-84)) & 127) : (Int64(110) | 1));
        func_1731854046694_133(
            if (var_1731854046694_2266) { // block id: 2268
                var_1731854046694_2270
            } else { // block id: 2272
                ((Int32(Int32(-60)) & 127)..=(Int32(Int32(-54)) & 127) : (Int64(20) | 1))
            }).func_1731854046694_2275(
            try { // block id: 2282
                var_1731854046694_51;
                [((UInt64(UInt64(23)) & 127)..(UInt64(UInt64(46)) & 127) : (Int64(96) | 1)),
                    ((UInt64(UInt64(111)) & 127)..(UInt64(UInt64(71)) & 127) : (Int64(94) | 1)),
                    ((UInt64(UInt64(198)) & 127)..(UInt64(UInt64(235)) & 127) : (Int64(-72) | 1)),
                    ((UInt64(UInt64(210)) & 127)..(UInt64(UInt64(116)) & 127) : (Int64(-34) | 1)),
                    ((UInt64(UInt64(182)) & 127)..=(UInt64(UInt64(96)) & 127) : (Int64(-110) | 1)),
                    ((UInt64(UInt64(233)) & 127)..(UInt64(UInt64(43)) & 127) : (Int64(-64) | 1)),
                    ((UInt64(UInt64(201)) & 127)..=(UInt64(UInt64(12)) & 127) : (Int64(76) | 1)),
                    ((UInt64(UInt64(158)) & 127)..=(UInt64(UInt64(56)) & 127) : (Int64(10) | 1)),
                    ((UInt64(UInt64(94)) & 127)..(UInt64(UInt64(233)) & 127) : (Int64(-92) | 1)),
                    ((UInt64(UInt64(14)) & 127)..=(UInt64(UInt64(28)) & 127) : (Int64(81) | 1))]
            } finally { // block id: 2285
                [((UInt64(UInt64(183)) & 127)..=(UInt64(UInt64(40)) & 127) : (Int64(-72) | 1)),
                    ((UInt64(UInt64(213)) & 127)..(UInt64(UInt64(127)) & 127) : (Int64(64) | 1)),
                    ((UInt64(UInt64(132)) & 127)..=(UInt64(UInt64(147)) & 127) : (Int64(5) | 1)),
                    ((UInt64(UInt64(160)) & 127)..=(UInt64(UInt64(183)) & 127) : (Int64(-87) | 1)),
                    ((UInt64(UInt64(136)) & 127)..=(UInt64(UInt64(149)) & 127) : (Int64(-97) | 1)),
                    ((UInt64(UInt64(127)) & 127)..(UInt64(UInt64(33)) & 127) : (Int64(39) | 1)),
                    ((UInt64(UInt64(133)) & 127)..(UInt64(UInt64(21)) & 127) : (Int64(-95) | 1)),
                    ((UInt64(UInt64(145)) & 127)..(UInt64(UInt64(58)) & 127) : (Int64(94) | 1)),
                    ((UInt64(UInt64(152)) & 127)..(UInt64(UInt64(154)) & 127) : (Int64(-82) | 1)),
                    ((UInt64(UInt64(52)) & 127)..(UInt64(UInt64(137)) & 127) : (Int64(32) | 1))]
            })
        // CRC Check All Variables
        var crcCheck = CrcCheck(5000, 32)
        crcCheck.updateBuffer(UInt64(var_1731854046694_91 * var_1731854046694_91), 64)
        crcCheck.updateBuffer(UInt64(var_1731854046694_102 * var_1731854046694_102), 64)
        crcCheck.updateBuffer(UInt64(var_1731854046694_1977 * var_1731854046694_1977), 64)
        crcCheck.updateBuffer(UInt64(var_1731854046694_1985 * var_1731854046694_1985), 64)
        crcCheck.updateBuffer(UInt64(var_1731854046694_4157 * var_1731854046694_4157), 64)
        crcCheck.crcCheck()
        crcCheck.getCrcOutput()
        ()
    } catch (e: NoneValueException) {
        println("Exception: NoneValueException")
    }

    return 0;
}

class CrcCheck {
    var buffer: Array<Int64>
    var poly: Array<Int64>
    var crc_output: Array<Int64>
    var poly_n: Int64
    var buffer_index: Int64
    var crc_output_n: Int64

    init(buffer_n: Int64, crc_n: Int64) {
        buffer_index = 0
        crc_output_n = 0
        crc_output = Array<Int64>(crc_output_n, {i => 0})
        buffer = Array<Int64>(buffer_n, {i => 0})
        poly_n = crc_n + 1
        if (crc_n == 32) {
            poly = [1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 1]
        } else if (crc_n == 16) {
            poly = [1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1]
        } else if (crc_n == 8) {
            poly = [1, 0, 0, 0, 0, 0, 1, 1, 1]
        } else {
            poly = [1, 1, 0, 0, 1]
        }
    }

    func updateBuffer(input: UInt64, byte_n: Int64): Unit {
        var value = input
        if (byte_n == 64) {
            value = value & 0xffffffffffffffff
        } else if (byte_n == 32) {
            value = value & 0xffffffff
        } else if (byte_n == 16) {
            value = value & 0xffff
        } else {
            value = value & 0xff
        }
        var byteArray = Array<Int64>(byte_n, {i => 0})
        var byte_c = 1
        var byte_index = 0
        while (value != 0) {
            if (value % 2 == 1) {
                byte_index = byte_n - byte_c
                byteArray[byte_index] = 1
            }
            byte_c++
            value = value >> 1
        }
        var i = 0
        while (i + byte_index < byte_n) {
            buffer[(i + buffer_index) % buffer.size] = byteArray[i + byte_index]
            i++
        }
        buffer_index = (buffer_index + i) % buffer.size
    }

    func updateBuffer(input: Bool, byte_n: Int64): Unit {
        if (input == true) {
            updateBuffer(1, byte_n)
        } else {
            updateBuffer(0, byte_n)
        }
    }

    func updateBuffer(input: Rune, byte_n: Int64): Unit {
        updateBuffer(UInt64(UInt32(input)), byte_n)
    }

    func updateBuffer(input: String, byte_n: Int64): Unit {
        for (c in input) {
            updateBuffer(c, byte_n)
        }
    }

    func updateBuffer(input: Unit, byte_n: Int64): Unit {
        updateBuffer(input.toString(), byte_n)
    }

    func updateBuffer(input: Any, byte_n: Int64): Unit {
        if (input is Hashable) {
            match (input as Hashable) {
                case Some(v) => updateBuffer(UInt64(v.hashCode()), byte_n)
                case _ => updateBuffer(1, byte_n)
            }
        } else if (input is ToString) {
            match (input as ToString) {
                case Some(v) => updateBuffer(v.toString(), byte_n)
                case _ => updateBuffer(1, byte_n)
            }
        } else {
            updateBuffer(1, byte_n)
        }
    }
    func crcCheck(): Unit {
        crc_output_n = buffer_index + poly_n - 1
        var crc_buffer = Array<Int64>(crc_output_n, {i => 0})
        var i = 0
        while (i < buffer_index) {
            crc_buffer[i] = buffer[i]
            i++
        }
        i = 0
        while (i < buffer_index) {
            if (crc_buffer[i] == 1) {
                var j = 0
                while (j < poly_n) {
                    crc_buffer[j + i] = crc_buffer[j + i] ^ poly[j]
                    j++
                }
            }
            i++
        }
        let check_code = crc_buffer[buffer_index..]
        var byte_add = 3 - crc_output_n % 3
        crc_output_n = byte_add + crc_output_n
        crc_output = Array<Int64>(crc_output_n, {i => 0})
        i = 0
        while (i < crc_output_n - byte_add) {
            if (i < buffer_index) {
                crc_output[i + byte_add] = buffer[i]
            } else {
                crc_output[i + byte_add] = check_code[i - buffer_index]
            }
            i++
        }
    }

    func getCrcOutput(): Unit {
        var i = 0
        while (i < crc_output_n) {
            var hex_value = 2 ** 2 * crc_output[i] + 2 ** 1 * crc_output[i + 1] + 1 * crc_output[i + 2]
            print(hex_value.toString())
            i = i + 3
        }
        print("\n")
    }
}
