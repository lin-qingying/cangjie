// DEPENDENCE: ../level_syscap_check/apilevel.cj device_info.cj dep.cj dep1.cj

import dep.*
import ohos.device_info.DeviceInfo

main() {
    @IfAvailable(level: 19, {=> A().a; func1() }, {=> func2()})
    0
}
