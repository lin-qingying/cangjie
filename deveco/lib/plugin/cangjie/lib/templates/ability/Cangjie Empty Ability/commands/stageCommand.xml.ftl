<copy src="../../common/media/startIcon.png" dest="${rootOut}/src/main/resources/base/media/startIcon.png"/>
<instantiate src="code template/stageMode/base-module.json5.ftl" dest="${rootOut}/src/main/module.json5"/>
<copy src="code template/resources/profile/main_pages.json" dest="${rootOut}/src/main/resources/base/profile/main_pages.json"/>
<copy src="code template/stageMode/module-build-profile.json5" dest="${rootOut}/build-profile.json5"/>
<merge src="code template/stageMode/module.json5.ftl" dest="${rootOut}/src/main/module.json5"/>

