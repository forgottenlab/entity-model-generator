# 更新日志

本文档记录本项目的重要版本变化。

格式参考 Keep a Changelog。

## [1.2.1] - 2026-10-07

### 修复

- 通过生成产物 provenance（来源追踪）机制增强增量 Annotation Processing 安全性。
- 区分 EMG 自身生成的 View、用户定义类型以及外部依赖类型。
- 避免静默复用已经过期的生成产物。
- 当生成结构与当前源码不一致时提供明确诊断。

### 新增

- 新增内部 `@EmgGenerated` 生成来源标记。
- 新增增量 javac/APT 回归测试。

### 兼容性

- 未破坏公开 Annotation API。
- 已由 1.2.0 生成的产物升级后需要执行一次 clean regeneration。