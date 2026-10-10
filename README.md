# Unciv 安卓城市队列购买

基于官方Unciv 4.22.7源码制作的独立Android客户端，增加城市“购买队列”按钮。不是JSON模组，不能在游戏“从URL下载”里安装，需要下载发布页APK后由Android安装。

独立包名：`com.unciv.bulkpurchase`，应用名“Unciv 队列购买”。可与原版并存；原版模组和存档需要重新下载或通过原版导出、在此应用导入。不自动读取另一个应用的私有数据。

将建筑和单位加入自己城市的生产队列，点击“购买队列”，确认一次后逐项购买。正常扣金币，保留原价、折扣、科技、资源、城市状态、奇观禁购和单位放置限制。金币不足等项目跳过并说明原因；同类单位可能需要先移走已购单位再买下一个。购买期间原版自动补入的项目不会额外购买。

可使用原版4.22.7支持的模组，国际合作组织需要单独在新客户端安装并启用。非官方客户端，与原版游戏开发者无隶属关系。

源码：`patch/`为新增Kotlin购买逻辑、城市弹窗与原生引擎测试；`apply_patch.py`向固定版本官方源码应用修改；`upstream.json`记录官方版本、提交和界面源文件哈希。

构建：获取upstream.json记录的官方源码，运行`python3 apply_patch.py 源码目录`，提供`android/bulk-purchase.jks`签名库（alias为bulk-purchase）与`UNCIV_SIGNING_PASSWORD`环境变量，再运行`:tests:test --tests com.unciv.logic.city.BulkPurchaseTest :android:assembleRelease`。Android SDK与Java21要求见官方构建文件。

官方源码：https://github.com/yairm210/Unciv/tree/4.22.7 。修改按MPL-2.0提供，保留原版许可证。最终安装包与验证范围以正式发布说明为准。
