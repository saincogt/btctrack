# 🎉 btctrack v2.7 已完成！

## 你要求的功能已全部实现

### ✅ 1. 三层结构（钱包 → 账户 → 地址）

**之前的问题：**
```
Trezor Alex
Trezor Default  
Trezor HODL
Cold Wallet HODL
Darwin Mongol
```
→ 分组太多，看起来很乱

**现在的解决方案：**
```
Trezor                    [钱包层]
  1.50000000 BTC
  
  Alex                    [账户层]
    0.15000000 BTC
    └─ 地址1              [地址层]
    └─ 地址2
    
  Default                 [账户层]
    0.80000000 BTC
    └─ 地址3
    
  HODL                    [账户层]
    1.55000000 BTC
    └─ 地址4

---

Cold Wallet              [钱包层]
  1.30000000 BTC
  
  HODL                   [账户层]
    └─ 地址5

---

Darwin                   [钱包层]
  Mongol                 [账户层]
    └─ 地址6
```

### ✅ 2. 自定义排序（不再是固定字母顺序）

**配置方法：**
```json
{
  "address": "bc1q...",
  "group": "Trezor/HODL",
  "order": 1          ← 数字越小越靠前
}
```

**示例：**
- `"order": 1` → Trezor 显示在最前面
- `"order": 2` → Cold Wallet 显示在第二
- `"order": 3` → Darwin 显示在第三
- 不设置 order → 默认 9999，显示在最后

---

## 🚀 如何升级你的配置

### 自动迁移脚本（最简单）

打开你的配置文件：
```bash
open -e /Users/zeal/Documents/study/projects/btctrack/plugins/.btcaddresses.json
```

然后使用**查找替换**功能：

| 查找 | 替换为 |
|------|--------|
| `"group": "Trezor Default"` | `"group": "Trezor/Default", "order": 1` |
| `"group": "Trezor Alex"` | `"group": "Trezor/Alex", "order": 1` |
| `"group": "Trezor Urga"` | `"group": "Trezor/Urga", "order": 1` |
| `"group": "Trezor HODL"` | `"group": "Trezor/HODL", "order": 1` |
| `"group": "Cold Wallet Decoy"` | `"group": "Cold Wallet/Decoy", "order": 2` |
| `"group": "Cold Wallet HODL"` | `"group": "Cold Wallet/HODL", "order": 2` |
| `"group": "Darwin Mongol"` | `"group": "Darwin/Mongol", "order": 3` |
| `"group": "Gift - Spend"` | `"group": "Gift/Spend", "order": 10` |

**重点：**
- 空格前的是钱包名，空格后的是账户名
- 用 `/` 连接：`钱包名/账户名`
- `order` 值决定显示顺序

---

## 📝 完整示例

**你当前的一条数据（旧格式）：**
```json
{
  "address": "bc1qj6rp7wt2a58ed0v27dw64netx4qr7el8ldtkpw",
  "group": "Trezor Default",
  "label": "Urga Zeus"
}
```

**升级后（新格式）：**
```json
{
  "address": "bc1qj6rp7wt2a58ed0v27dw64netx4qr7el8ldtkpw",
  "group": "Trezor/Default",
  "label": "Urga Zeus",
  "order": 1
}
```

**效果：**
- 这个地址会显示在 `Trezor → Default → Urga Zeus`
- Trezor 钱包会排在最前面（因为 order=1）

---

## 🎯 建议的 order 值

根据你的数据，建议这样设置：

| 钱包 | order | 原因 |
|------|-------|------|
| Trezor | 1 | 你的主钱包，应该最优先显示 |
| Cold Wallet | 2 | 第二重要的钱包 |
| Darwin | 3 | 第三优先 |
| Gift | 10 | 礼物地址，不常用，放后面 |

---

## ⚡ 快速测试

### 1. 备份现有配置
```bash
cp /Users/zeal/Documents/study/projects/btctrack/plugins/.btcaddresses.json ~/.btcaddresses.json.backup
```

### 2. 编辑配置
从 SwiftBar 菜单：**✎ Edit Config → TextEdit**

### 3. 应用更改
保存后，SwiftBar 会自动刷新

### 4. 如果出错，恢复备份
```bash
cp ~/.btcaddresses.json.backup /Users/zeal/Documents/study/projects/btctrack/plugins/.btcaddresses.json
```

---

## 📚 详细文档

我已经创建了三个文档帮助你：

1. **GUIDE_v2.7.md** — 完整使用指南（8KB，包含所有示例）
2. **MIGRATION_v2.7.md** — 从 v2.4 迁移的详细步骤（5KB）
3. **.btcaddresses.sample.v2.7.json** — 新格式示例配置

查看方式：
```bash
cd /Users/zeal/Documents/study/projects/btctrack
cat GUIDE_v2.7.md
```

---

## 🔧 技术改动

### 代码层面
- ✅ 更新 `btctrack.1h.sh` 到 v2.7（从 398 行扩展到 471 行）
- ✅ 新增 `order` 字段解析
- ✅ 重写 `organize.py` 支持三层结构
- ✅ 重写显示逻辑（WALLET/ACCOUNT/ADDRESS 标记）
- ✅ **完全向后兼容** v2.4 配置

### 隐私保护
- ✅ 查询顺序仍然是随机的（隐私不受影响）
- ✅ 显示顺序按你的配置排列（方便使用）
- ✅ 两者互不影响

### 测试状态
- ✅ Bash 语法检查通过
- ✅ Python 解析逻辑测试通过
- ✅ 组织逻辑测试通过
- ✅ 三层结构输出格式正确

---

## ❓ 常见问题

**Q: 我必须马上升级吗？**  
A: 不用。你的旧配置仍然可以正常工作。随时升级都可以。

**Q: 如果我不想要三层结构呢？**  
A: 继续用旧格式就行：`"group": "Personal"` 会显示为 2 层。

**Q: 可以混用新旧格式吗？**  
A: 可以！同一个配置文件里可以有：
- `"group": "Personal"` （2层）
- `"group": "Trezor/HODL"` （3层）

**Q: order 必须连续吗？**  
A: 不用。可以是 1, 2, 10, 99 都行。只要记住：数字越小越靠前。

**Q: 不设置 order 会怎样？**  
A: 默认是 9999，按字母排序，显示在最后。

---

## ✨ 下一步

1. **先看效果** — 我已经部署了新代码，SwiftBar 会在下次刷新时使用新版本
2. **测试旧配置** — 确认你的地址仍然正常显示（应该是 2 层结构）
3. **逐步迁移** — 选几个地址先改成 `钱包/账户` 格式，看看效果
4. **调整顺序** — 添加 `order` 字段，把重要的钱包放前面

需要帮助随时告诉我！

---

**版本：** v2.7  
**日期：** 2026-03-01  
**状态：** ✅ 已完成并测试
