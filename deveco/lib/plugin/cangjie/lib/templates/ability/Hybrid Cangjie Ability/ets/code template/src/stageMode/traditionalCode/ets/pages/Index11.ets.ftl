import { requireCJLib } from "libark_interop_loader.so";
import { register_hsp_api } from 'cjhyapiregister';

register_hsp_api()

interface CJLib {
  testCJ(src: string): string
}

@Entry
@Component
struct Index {
  @State message: string = 'Hello World';

  build() {
    RelativeContainer() {
      Text(this.message)
        .fontSize(40)
        .fontWeight(FontWeight.Bold)
        .alignRules({
          center: { anchor: '__container__', align: VerticalAlign.Center },
          middle: { anchor: '__container__', align: HorizontalAlign.Center }
        })
        .onClick(() => {
          const lib = requireCJLib("libohos_app_cangjie_${moduleName}.so") as CJLib
          this.message = lib.testCJ("Cangjie")
        })
    }
    .height('100%')
    .width('100%')
  }
}