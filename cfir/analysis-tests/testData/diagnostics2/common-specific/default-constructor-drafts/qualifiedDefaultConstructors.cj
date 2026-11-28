package default_constructor_qualified

import default_constructor_models as Models

// 限定包名的 constructor 调用必须走实际导入声明，不用短名或活动普通函数冒充。
public interface QualifiedConstructorSource {
    func defaultClass(): Models.DefaultClass<Int64, Bool> {
        return Models.DefaultClass<Int64, Bool>()
    }

    func defaultStruct(): Models.DefaultStruct<Int64, Bool> {
        return Models.DefaultStruct<Int64, Bool>()
    }

    func staticClass(): Models.StaticClass {
        return Models.StaticClass()
    }

    func staticStruct(): Models.StaticStruct {
        return Models.StaticStruct()
    }

    func explicitClass(): Models.ExplicitClass {
        return Models.ExplicitClass()
    }

    func primaryClass(value: Int64): Models.PrimaryClass {
        return Models.PrimaryClass(value)
    }

    func primaryStruct(value: Int64, flag: Bool): Models.PrimaryStruct {
        return Models.PrimaryStruct(value, flag)
    }
}
