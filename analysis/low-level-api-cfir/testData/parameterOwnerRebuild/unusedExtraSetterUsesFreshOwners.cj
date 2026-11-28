// 源码保留 get/set，测试仅从实际 raw 属性移除 setter。A/M 未声明，不期待语义诊断。
package lowlevel.parameterownerrebuild

class Owner {
    public mut prop value: Int64 {
        get() { return @M(1) }
        set(@!A incoming) { @M(incoming) }
    }
}
