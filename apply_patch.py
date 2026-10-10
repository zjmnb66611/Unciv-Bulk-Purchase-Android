"""在固定版本的官方源码上应用安卓补丁，匹配失败即停止。"""
from pathlib import Path
import hashlib
import json
import shutil
import sys

HERE = Path(__file__).resolve().parent
UPSTREAM = Path(sys.argv[1]).resolve()
manifest = json.loads((HERE / 'upstream.json').read_text(encoding='utf-8'))

def replace_once(path, old, new):
    content = path.read_text(encoding='utf-8')
    if content.count(old) != 1:
        raise RuntimeError(f'版本或源码不匹配：{path.name}')
    path.write_text(content.replace(old, new), encoding='utf-8')

target = UPSTREAM / 'core/src/com/unciv/ui/screens/cityscreen/CityConstructionsTable.kt'
if hashlib.sha256(target.read_bytes()).hexdigest() != manifest['cityTableSha256']:
    raise RuntimeError('城市界面源码与已校验4.22.7版本不同')
anchor = '    private fun updateAvailableConstructions() {'
# 直接在按钮更新结束处插入，放在单独一行以适配手机宽度。
content = target.read_text(encoding='utf-8')
start = content.index('    private fun updateButtons(')
end = content.index('\n    private fun updateConstructionQueue()', start)
block = content[start:end]
closing = block.rfind('\n    }')
if closing < 0 or 'BulkPurchaseUI' in block:
    raise RuntimeError('按钮函数结构不符合预期')
addition = '''
        if (cityView.isOwnedByViewer()) {
            buttonsTable.row()
            buttonsTable.add(BulkPurchaseUI.button(cityScreen))
                .colspan(buttonsTable.columns.coerceAtLeast(1)).fillX().minWidth(180f).minHeight(44f).padTop(6f)
        }
'''
updated = block[:closing] + addition + block[closing:]
target.write_text(content[:start] + updated + content[end:], encoding='utf-8')

for path in (HERE / 'patch').rglob('*'):
    if path.is_file():
        destination = UPSTREAM / path.relative_to(HERE / 'patch')
        if destination.exists():
            raise RuntimeError(f'补丁文件意外覆盖原文件：{destination.name}')
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, destination)

android = UPSTREAM / 'android/build.gradle.kts'
replace_once(android, 'applicationId = BuildConfig.identifier', 'applicationId = "com.unciv.bulkpurchase"')
replace_once(android, '    buildTypes {', '''    signingConfigs {
        create("bulkPurchase") {
            storeFile = rootProject.file("android/bulk-purchase.jks")
            storePassword = System.getenv("UNCIV_SIGNING_PASSWORD")
            keyAlias = "bulk-purchase"
            keyPassword = System.getenv("UNCIV_SIGNING_PASSWORD")
        }
    }

    buildTypes {''')
replace_once(android, '        release {', '        release {\n            signingConfig = signingConfigs.getByName("bulkPurchase")')

# 使用独立应用名；保留原namespace，源码的资源引用保持有效。
for path in (UPSTREAM / 'android/res').glob('values*/strings.xml'):
    content = path.read_text(encoding='utf-8')
    import re
    content = re.sub(r'(<string name="app_name">).*?(</string>)', r'\1Unciv 队列购买\2', content)
    path.write_text(content, encoding='utf-8')
print('已应用4.22.7安卓购买补丁；独立包名com.unciv.bulkpurchase。')
