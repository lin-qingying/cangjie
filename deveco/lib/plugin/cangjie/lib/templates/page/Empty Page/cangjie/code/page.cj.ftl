package ${cjPackageName}

import ohos.component.Text
import ohos.component.Button
import ohos.component.Column
import ohos.component.CustomView
import ohos.state_manage.LocalStorage
import ohos.state_manage.ObservedProperty
import ohos.state_manage.SubscriberManager
import ohos.state_manage.ViewStackProcessor
import ohos.state_macro_manage.State
import ohos.state_macro_manage.Component
import ohos.state_macro_manage.HybridComponentEntry
import ohos.hybrid_base.CJPageEntry
import ohos.hybrid_base.HybridComponentBase
import cj_res_${moduleName}.app

@HybridComponentEntry
@Component
class ${pageName?cap_first} {
    @State
    var msg: String = "Hello"

    public func build() {
        Column {
            Text(msg)
            Button("click to change Text").onClick {
                => msg = "world"
            }
        }
    }
}
