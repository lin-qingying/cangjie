import { CJHybridComponentV2 } from 'cjhybridview';
import { register_hsp_api } from 'cjhyapiregister';

register_hsp_api()

<#if moduleType = "entry" || moduleType = "feature">
@Entry
@Component
struct ${pageName?cap_first} {
  @State message: string = 'Hello World';

  build() {
    Column() {
      CJHybridComponentV2({
        library: '${cjDynamicName}',
        component: '${pageName?cap_first}'
      })
    }
    .height('100%')
    .width('100%')
  }
}
<#else>
@Component
export struct ${pageName?cap_first} {
  @State message: string = 'Hello World';

  build() {
    Column() {
      CJHybridComponentV2({
        library: '${cjDynamicName}',
        component: '${pageName?cap_first}'
      })
    }
    .height('100%')
    .width('100%')
  }
}
</#if>