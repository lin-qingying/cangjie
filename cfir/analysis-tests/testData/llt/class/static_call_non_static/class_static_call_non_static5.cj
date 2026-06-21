


internal import std.collection.*

var map_top_2: HashMap<Int32, Rune> = HashMap<Int32, Rune>([(-29, r'C'), (-10, r'a'), (76, r'N')])
var arrayInt32: Array<Int32> = [321, -322, 10, -123, -98]

main() {
    let class_sub_ins = Class_sub_1()
    var map_main_1: HashMap<UInt16, Int64> = class_sub_ins.map_subCla_1 //子类成员变量
    var map_main_2: HashMap<Int32, Rune> = class_sub_ins.fun_sub_c1(map_top_2) //子类实例函数
    var map_main_3: HashMap<Int32, Rune> = Class_sub_1.map_cla_2 // 父类静态成�?    var map_main_4: HashMap<Int32, Rune> = class_sub_ins.fun_c1(map_top_2, par_2: r'H') // 父类实例成员函数
    var map_main_5: HashMap<Int32, Rune> = Class_sub_1.fun_cs1(12, r'Z') // 父类静态成员函�?
    let map_main_6: HashMap<Int32, Rune> = Class_sub_1.fun_sub_cs1(r'G') //子类自有静态函�?
    return -1
}

open class Clazz_1 {
    public var width: Int32 = arrayInt32[0]
    public var length: Int32 = arrayInt32[2]

    public Clazz_1(para_f1: Int32, length: Int32) {
        this.width = para_f1
        this.length = length
    }
    public init() {}
    private var map_cla_1: HashMap<Int64, UInt64> = HashMap<Int64, UInt64>([(-719, 17550), (230, 18672), (-106, 3945)])
    public static var map_cla_2: HashMap<Int32, Rune> = HashMap<Int32, Rune>([(-279, r'C'), (30, r'a'), (16, r'N')])
    public static func fun_cs1(par_1: Int32, par_2: Rune): HashMap<Int32, Rune> {
        var map_fs1: HashMap<Int32, Rune> = map_cla_2
        map_fs1.add(par_1, par_2)
        map_fs1.add(Int32(map_fs1.size), r'G')
        return map_fs1
    }

    public open func fun_c1(par_1: HashMap<Int32, Rune>, <!CANNOT_HAVE_DEFAULT_PARAM!>par_2!: Rune = r'J'<!>): HashMap<Int32, Rune> {
        var map_f1: HashMap<Int32, Rune> = par_1
        map_f1.add(Int32(map_f1.size), par_2)
        map_f1[12] = r'H'
        return map_f1
    }
}

open class Class_sub_1 <: Clazz_1 {
    private var vP1: Int32 = 12
    public let para_sub1: Int32 = 23
    public static var para_sub2: Int32 = -90
    public Class_sub_1() {
        super(110, -120)
    }

    public var clazz_1_ins = Clazz_1() //父类的实例对�?    public static let sub_cp1: HashMap<Int32, Rune> = Class_sub_1.map_cla_2 //父类成员变量
    public var map_subCla_1: HashMap<UInt16, Int64> = HashMap<UInt16, Int64>([(12, 17750), (23, 1836), (56, 3594)])

    public static func fun_sub_cs1(par_1: Rune): HashMap<Int32, Rune> {
        var map_subcla_fun_s1: HashMap<Int32, Rune> = Clazz_1.fun_cs1(Int32(<!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>m<!>ap_subCla_1[23]), r'Z')
        map_subcla_fun_s1.add(Int32(map_subcla_fun_s1.size), par_1)
        return map_subcla_fun_s1
    }
    /*
     */
    public func fun_sub_c1(par_1: HashMap<Int32, Rune>): HashMap<Int32, Rune> {
        var map_cla_fun_sub1: HashMap<Int32, Rune> = clazz_1_ins.fun_c1(par_1) //父类
        map_cla_fun_sub1.add(Int32(map_cla_fun_sub1.size), r'O')
        return map_cla_fun_sub1
    }
}
