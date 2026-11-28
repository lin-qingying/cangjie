// static/open 组合故意非法：STATUS 移除无效 OPEN 语义，同时保留实际写过 modifier 的来源事实。
open class Host {
    public static open func invalidOpen(): Unit {}
    public static open prop value: Int32 {
        get() { return 1 }
    }
}
