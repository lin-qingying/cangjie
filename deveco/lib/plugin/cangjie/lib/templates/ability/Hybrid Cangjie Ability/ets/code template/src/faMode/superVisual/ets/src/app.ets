import hilog from '@ohos.hilog';

class MainAbility {
  onCreate() {
    hilog.info(0x0000, 'testTag', '%{public}s', 'Application onCreate');
  }
  onDestroy() {
    hilog.info(0x0000, 'testTag', '%{public}s', 'Application onDestroy');
  }
}

export default new MainAbility();