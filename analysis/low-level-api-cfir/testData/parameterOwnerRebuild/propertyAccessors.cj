// mut 属性具有 get/set；setter 参数只有名字并保留前缀注解。A/M 未声明，不期待语义诊断。
package lowlevel.parameterownerrebuild

class Owner {
    public mut prop value: Int64 {
        get() { return @M(1) }
        set(@!A incoming) { @M(incoming) }
    }
}
