# todo

- [ClickHouse 字段级血缘设计文档](docs/clickhouse-calcite-lineage.md)

## Java MVP 代码

新增一个可编译运行的最小字段血缘实现（不依赖 Calcite），用于承载规则与 DAG 结构：

- `src/main/java/com/todo/lineage/LineageService.java`
- `src/main/java/com/todo/lineage/LineageGraph.java`
- `src/main/java/com/todo/lineage/DemoMain.java`

### 本地编译与运行

```bash
javac src/main/java/com/todo/lineage/*.java
java -cp src/main/java com.todo.lineage.DemoMain
```

### 轻量测试

```bash
javac src/main/java/com/todo/lineage/*.java src/test/java/com/todo/lineage/LineageServiceTestMain.java
java -cp src/main/java:src/test/java com.todo.lineage.LineageServiceTestMain
```
