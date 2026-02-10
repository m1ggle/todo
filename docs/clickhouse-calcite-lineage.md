# 基于 Apache Calcite 的 ClickHouse 字段级血缘设计

## 1. 一句话架构

使用 Apache Calcite 完成 **SQL 解析 + RelNode 关系代数建模**，在 RelNode 上做访问与映射，产出字段级血缘 DAG。

```text
ClickHouse SQL
   ↓
Calcite Parser（ClickHouse 方言）
   ↓
SqlNode AST
   ↓
SqlToRelConverter
   ↓
RelNode Tree（Project / Join / Aggregate / TableScan）
   ↓
RelVisitor / RelShuttle
   ↓
Column -> Column Lineage DAG
```

---

## 2. 为什么字段级血缘必须基于 RelNode

仅依赖 AST 更适合表级血缘。字段级血缘需要表达式级、输入列级、算子级追踪，RelNode 更稳定。

| 能力 | 纯 AST | Calcite RelNode |
|---|---|---|
| 列裁剪 / 投影展开 | ❌ | ✅ |
| Join / 子查询字段对齐 | ❌ | ✅ |
| 聚合输入输出映射 | ❌ | ✅ |

结论：字段级血缘是关系代数层问题，不是语法树层问题。

---

## 3. ClickHouse 兼容点与风险

ClickHouse 语法包含大量扩展，推荐提前分层兼容。

重点语法：

- `FINAL`
- `ARRAY JOIN`
- `LIMIT BY`
- `GLOBAL JOIN`
- `WITH ... AS`
- `dictGet(...)`
- 类型与条件函数（如 `toUInt64(...)`, `if(...)`）

### 兼容策略

- **Parser 扩展（主路径）**：扩展 Calcite grammar（生产推荐）
- **SQL 预处理（兜底）**：对不可解析语法做 rewrite（如 `FINAL` 删除、`LIMIT BY` 降级）
- 建议采用 **A + B 混合策略**

---

## 4. Calcite 端模块设计

### 4.1 方言层（可选但建议）

```java
public class ClickHouseSqlDialect extends SqlDialect {
    public static final ClickHouseSqlDialect DEFAULT =
        new ClickHouseSqlDialect(Context.DEFAULT);

    protected ClickHouseSqlDialect(Context context) {
        super(context);
    }
}
```

用途：函数名渲染、类型显示、SQL pretty-print、一致化输出。

### 4.2 Parser 层（核心）

#### 路线 A：扩展 Parser.jj

补充关键 token / 语法规则：

- `ARRAY JOIN`
- `LIMIT BY`
- `FINAL`

#### 路线 B：预处理

- 删除 `FINAL`
- `ARRAY JOIN` 重写为可识别结构
- `LIMIT BY` 改写为子查询 + window

---

## 5. 从 SQL 到 RelNode

```java
FrameworkConfig config = Frameworks.newConfigBuilder()
    .parserConfig(SqlParser.config().withLex(Lex.MYSQL))
    .defaultSchema(schema)
    .build();

Planner planner = Frameworks.getPlanner(config);

SqlNode sqlNode = planner.parse(sql);
SqlNode validated = planner.validate(sqlNode);
RelNode relNode = planner.rel(validated).project();
```

产物是逻辑计划树，字段级血缘直接在该树上构建。

---

## 6. 血缘数据模型

```java
class ColumnLineage {
    String sourceTable;
    String sourceColumn;
    String targetTable;
    String targetColumn;
    String expression; // 如 a + b, sum(x)
    LineageType type;  // DIRECT / DERIVED / AGG / EXTERNAL / UNKNOWN
}
```

建议补充：

- `queryId` / `jobId`
- `confidence`
- `operatorPath`（定位来源算子）

---

## 7. RelNode 访问框架

```java
class LineageVisitor extends RelVisitor {
    @Override
    public void visit(RelNode node, int ordinal, RelNode parent) {
        if (node instanceof LogicalProject) {
            handleProject((LogicalProject) node);
        } else if (node instanceof LogicalJoin) {
            handleJoin((LogicalJoin) node);
        } else if (node instanceof LogicalAggregate) {
            handleAggregate((LogicalAggregate) node);
        } else if (node instanceof TableScan) {
            handleScan((TableScan) node);
        }
        super.visit(node, ordinal, parent);
    }
}
```

---

## 8. 字段血缘生成规则（核心）

### 8.1 TableScan：血缘起点

- 读取 `qualifiedName`
- 读取 `rowType.fieldNames`
- 为每列创建 `ColumnRef(table, column)`

### 8.2 Project：表达式展开

示例：

```sql
SELECT a + b AS c FROM t;
```

规则：

- 逐个输出列读取 `RexNode`
- 递归提取 `RexInputRef` 作为源列
- `RexCall` 记录函数/运算表达式

输出血缘：

- `t.a -> result.c (DERIVED, expr=a+b)`
- `t.b -> result.c (DERIVED, expr=a+b)`

### 8.3 Join：左右输入字段对齐

- `left index < leftFieldCount`
- `right index = globalIndex - leftFieldCount`

对 `ON` 条件建议单独记录为条件依赖（可选）。

### 8.4 Aggregate：聚合映射

示例：

```sql
SELECT dept, sum(salary) FROM emp GROUP BY dept;
```

规则：

- `GROUP BY` 列：`DIRECT`
- 聚合输出列：从 `AggregateCall.argList` 回溯输入列，标记 `AGG`

### 8.5 WITH / 子查询

Calcite 会展开为嵌套 RelNode。

- 统一做 DFS / 后序归并
- 子查询输出列映射至外层输入列

---

## 9. ClickHouse 特殊能力处理建议

### 9.1 ARRAY JOIN

`ARRAY JOIN arr AS a`

- 建议记录：`arr -> a`
- 类型：`DERIVED_ARRAY`

### 9.2 dictGet / 外部字典

`dictGet('dim', 'name', id)`

- 标记 `EXTERNAL_LINEAGE`
- 不强制追踪到外部系统内部字段

### 9.3 FINAL

- 建议在血缘层忽略
- 对字段映射无影响

---

## 10. 输出 DAG 结构建议

```json
{
  "nodes": ["t1.a", "t1.b", "result.c"],
  "edges": [
    {
      "from": "t1.a",
      "to": "result.c",
      "type": "DERIVED",
      "expr": "a + b"
    }
  ]
}
```

可扩展字段：

- `sqlText`
- `operatorId`
- `confidence`
- `sourceSystem`

---

## 11. 生产增强建议

### 11.1 稳定性

- 解析失败时退化 AST 能力（至少保表级血缘）
- 未识别函数标记 `UNKNOWN`

### 11.2 性能

- Planner 复用
- SQL Hash 结果缓存
- 子查询 lineage 结果缓存

### 11.3 可解释性

- 必须存 `expression`
- 前端展示算子路径与表达式，避免“黑盒血缘”

---

## 12. 落地建议（最小可用版本）

第一阶段（2~3 周）可交付：

1. 支持 `SELECT-FROM-WHERE-JOIN-GROUP BY` 主路径
2. 支持 `WITH` 与单层子查询
3. 支持常见函数表达式解析
4. 输出字段级 DAG JSON
5. 提供失败回退与 `UNKNOWN` 标记

后续再扩展 ClickHouse 特性（`ARRAY JOIN`, `LIMIT BY`, `dictGet`）。

---

## 13. Java 落地代码骨架（MVP 可直接开工）

```java
public final class LineageService {

    private final Planner planner;

    public LineageService(SchemaPlus schema) {
        FrameworkConfig config = Frameworks.newConfigBuilder()
            .parserConfig(SqlParser.config().withLex(Lex.MYSQL))
            .defaultSchema(schema)
            .build();
        this.planner = Frameworks.getPlanner(config);
    }

    public LineageGraph analyze(String sql) {
        try {
            SqlNode parsed = planner.parse(sql);
            SqlNode validated = planner.validate(parsed);
            RelNode rel = planner.rel(validated).project();

            LineageBuilder builder = new LineageBuilder();
            new LineageRelVisitor(builder).go(rel);
            return builder.build();
        } catch (Exception ex) {
            return LineageGraph.fallbackUnknown(sql, ex.getMessage());
        }
    }
}
```

关键实现建议：

- `LineageRelVisitor`：只做遍历与分发；
- `LineageBuilder`：维护 `RelNode -> 输出列来源集合` 的中间态；
- `RexLineageExtractor`：递归解析 `RexNode`，统一处理函数、算术、CASE；
- `FallbackAnalyzer`：解析失败时输出表级或 UNKNOWN 级别边。

---

## 14. 端到端示例（SQL -> 边集合）

输入 SQL：

```sql
WITH x AS (
  SELECT user_id, amount, toUInt64(ts) AS ts_u64
  FROM ods.orders FINAL
)
SELECT
  user_id,
  sum(amount) AS total_amt,
  max(ts_u64) AS max_ts
FROM x
GROUP BY user_id;
```

建议输出（简化）：

```json
{
  "edges": [
    {"from": "ods.orders.user_id", "to": "result.user_id", "type": "DIRECT"},
    {"from": "ods.orders.amount", "to": "result.total_amt", "type": "AGG", "expr": "sum(amount)"},
    {"from": "ods.orders.ts", "to": "result.max_ts", "type": "DERIVED", "expr": "toUInt64(ts)"},
    {"from": "x.ts_u64", "to": "result.max_ts", "type": "AGG", "expr": "max(ts_u64)"}
  ],
  "warnings": [
    "FINAL ignored for lineage"
  ]
}
```

说明：

- `FINAL` 不影响字段映射，只记录 warning；
- `toUInt64(ts)` 先产生 `DERIVED`，再被 `max()` 聚合为 `AGG`；
- CTE `x` 作为中间层在图中可保留也可折叠。

---

## 15. 规则优先级与冲突处理（建议）

当同一目标列可由多条路径推导时，建议按优先级打标：

1. `DIRECT`（纯透传）
2. `DERIVED`（表达式）
3. `AGG`（聚合）
4. `EXTERNAL_LINEAGE`（外部函数）
5. `UNKNOWN`（兜底）

冲突处理：

- **保留全部边**，通过 `type + confidence` 区分；
- UI 默认展示最高置信度路径，其余作为“展开”；
- 对 `UNKNOWN` 边强制提示，便于后续补齐规则。
