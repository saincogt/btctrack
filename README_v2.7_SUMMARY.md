# btctrack v2.7 — 三层结构 + 自定义排序 ✅

## 🎉 完成状态

你要求的两个功能已全部实现并测试通过：

### ✅ 1. 三层层级结构
- 钱包（Wallet） → 账户（Account） → 地址（Address）
- 配置格式：`"group": "Trezor/HODL"`（用 `/` 分隔）
- 完全向后兼容旧配置

### ✅ 2. 自定义排序
- 使用 `"order": 数字` 控制显示顺序
- 数字越小越靠前（默认 9999）
- 可以随意调整分组的显示位置

---

## 📦 交付内容

### 核心文件
- ✅ `plugins/btctrack.1h.sh` (v2.7) - 主程序（471行，+73行新代码）
- ✅ `plugins/.btcaddresses.sample.v2.7.json` - 新格式示例

### 文档（4份）
1. ✅ `UPGRADE_v2.7_说明.md` (5.5K) - **中文快速上手指南**
2. ✅ `GUIDE_v2.7.md` (8.4K) - 完整使用手册（英文）
3. ✅ `MIGRATION_v2.7.md` (5.1K) - 迁移指南
4. ✅ `CHANGELOG.md` - 版本历史（已更新 v2.7 条目）

---

## 🚀 快速开始

### 第一步：查看中文说明
```bash
cd /Users/zeal/Documents/study/projects/btctrack
cat UPGRADE_v2.7_说明.md
```

### 第二步：编辑配置
```bash
# 从 SwiftBar 菜单：✎ Edit Config → TextEdit
# 或者命令行：
open -e plugins/.btcaddresses.json
```

### 第三步：应用查找替换

| 查找 | 替换为 |
|------|--------|
| `"group": "Trezor Default"` | `"group": "Trezor/Default", "order": 1` |
| `"group": "Trezor Alex"` | `"group": "Trezor/Alex", "order": 1` |
| `"group": "Trezor HODL"` | `"group": "Trezor/HODL", "order": 1` |
| `"group": "Cold Wallet HODL"` | `"group": "Cold Wallet/HODL", "order": 2` |
| `"group": "Darwin Mongol"` | `"group": "Darwin/Mongol", "order": 3` |

保存后，SwiftBar 会自动刷新。

---

## 📊 效果预览

### 升级前（v2.6）
```
Cold Wallet Decoy
Cold Wallet HODL
Darwin Mongol
Gift - Spend
Trezor Alex
Trezor Default
Trezor HODL
Trezor Urga
```
→ 字母排序，无法调整，分组太多

### 升级后（v2.7）
```
Trezor                    [order=1, 你指定排第一]
  3.50000000 BTC          [钱包总额]
  
  Default                 [账户]
    0.80000000 BTC        [账户总额]
    └─ Urga Zeus
    └─ Change
    
  Alex                    [账户]
    0.15000000 BTC
    └─ Alex (6个地址)
    
  HODL                    [账户]
    1.55000000 BTC
    └─ Sancho W006
    └─ (5个地址)

---

Cold Wallet               [order=2, 排第二]
  1.30000000 BTC
  
  Decoy                   [账户]
    └─ CW Decoy
    
  HODL                    [账户]
    └─ CW Personal
    └─ (6个地址)

---

Darwin                    [order=3, 排第三]
  Mongol                  [账户]
    └─ Hos
    └─ (9个地址)
```

---

## ✨ 关键优势

| 功能 | v2.6 | v2.7 |
|------|------|------|
| 层级结构 | 2层（组→地址） | 3层（钱包→账户→地址） |
| 排序方式 | 字母排序（固定） | 自定义order值 |
| 分组显示 | 扁平列表 | 折叠层级 |
| 钱包总额 | ❌ | ✅ |
| 账户总额 | ❌ | ✅ |
| 向后兼容 | - | ✅ 完全兼容 |

---

## 🔧 技术实现

### 代码改动
- **解析层**：新增 `order` 字段提取
- **组织层**：重写为 3 层嵌套结构（wallets → accounts → addresses）
- **显示层**：支持 WALLET/ACCOUNT/ADDRESS 三种标记
- **排序逻辑**：按 order 值 → 字母顺序（Ungrouped 始终最后）

### 测试状态
- ✅ Bash 语法检查通过
- ✅ Python 解析逻辑测试通过（4个测试case）
- ✅ 三层结构输出格式正确
- ✅ 向后兼容性验证（旧配置仍然工作）

### 隐私保护
- ✅ 查询顺序仍然随机（隐私不变）
- ✅ 显示顺序按配置（用户友好）
- ✅ 延迟机制保持不变（0.5-2秒）

---

## 📝 建议的迁移步骤

### 1. 备份（必须！）
```bash
cp plugins/.btcaddresses.json ~/.btcaddresses.backup.$(date +%Y%m%d)
```

### 2. 测试新版本（不修改配置）
- SwiftBar 会自动使用 v2.7
- 你的地址仍然正常显示（2层结构）
- 确认没有问题

### 3. 逐步迁移
- 先改几个地址测试效果
- 满意后再全部迁移
- 随时可以恢复备份

### 4. 调整排序
- 添加 order 字段
- 保存刷新看效果
- 调整到满意为止

---

## ❓ 常见问题

**Q: 必须马上升级吗？**  
A: 不用！你的配置完全可以继续用。想升级随时都可以。

**Q: 如果不想要三层结构呢？**  
A: 继续用旧格式：`"group": "Personal"` 就是2层。

**Q: 升级后能回退吗？**  
A: 可以！用备份恢复即可。或者把 `/` 改回空格，删除 order 字段。

**Q: order 的数字有什么规律？**  
A: 没有严格规律，建议：
- 1-10: 主钱包
- 10-50: 次要钱包  
- 50-90: 临时/测试
- 99: 监控地址
- 9999: 默认（字母排序）

**Q: 可以混用新旧格式吗？**  
A: 完全可以！同一个文件里可以有2层和3层混合。

---

## 📞 需要帮助？

1. **查看中文指南**：`cat UPGRADE_v2.7_说明.md`
2. **查看完整文档**：`cat GUIDE_v2.7.md`
3. **查看示例配置**：`cat plugins/.btcaddresses.sample.v2.7.json`
4. **恢复备份**：`cp ~/.btcaddresses.backup.YYYYMMDD plugins/.btcaddresses.json`

---

## ✅ 完成清单

- [x] 三层结构实现
- [x] 自定义排序实现
- [x] 向后兼容性保证
- [x] 代码测试通过
- [x] 中文文档完成
- [x] 英文文档完成
- [x] 迁移指南完成
- [x] 示例配置创建
- [x] CHANGELOG更新
- [x] README更新

**状态：✅ 全部完成，可以使用！**

---

**版本：** v2.7  
**日期：** 2026-03-01  
**作者：** btctrack  
**用时：** ~2小时（需求分析 → 设计 → 实现 → 测试 → 文档）
