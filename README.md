# LruMM

an implementation of Lru-K algorithm

Lru-K算法的实现. 相比LruCache有更好的命中率, 能够防止缓存穿透, 同时降低缓存污染.

# 原理

LruCache的回收策略是"最近最少使用", 在数据周期性访问的场景下仍有优化空间. 我们希望增加访问次数这一维度, 提高被多次访问的数据的权重, 即最近访问过一次的数据应该比早先被多次访问的数据更早的回收. 

举例来说, 向缓存中插入数字1, 1, 1, 2, 3, 缓存池大小为2, LruCache会淘汰1, 剩下2, 3. 但1有更大可能在下次被访问, 期望的结果是淘汰2, 剩下3, 1.

# 实现
LruMM由Major和Minor两个LinkedHashMap组成, 总大小固定为maxSize. 

多次访问/写入的数据保存在Major, 单次的保存在Minor. Major和Minor分别以使用顺序排序, 最近使用的在末尾. 访问时先从Major的末尾向头部查询, 缓存满时优先从Minor头部淘汰. 

* 访问: 先从Major查询, 未命中则从Minor查询.
* 新增: 插入Minor. 
* 修改: 先从Major查询, 有则替换. 再查Minor, 无则插入. 
* 升级: 访问命中或修改Minor后将该数据从Minor移到Major末尾.
* 降级: 每次修改Major都会触发降级, 降级策略是当Major长度大于maxSize/2则将Major头部移到Minor末尾.

LruMM是线程安全的.

LruMM还解决了LruCache的缓存穿透问题. 当数据的值为空, LruCache无法缓存空值, 如果频繁查这个关键字会导致每次请求都绕过缓存, 发生缓存穿透. 因此LruMM允许插入空值.

# 用法

与LruCache一致.

* 获取实例
```java
LruMM<Object, Object> lruMM = new LruMM<>(maxSize);
```
* 写入缓存
```java
lruMM.put("key", "value");
```
* 获取缓存
```java
Object value = lruMM.get("key");
```
* 获取Major或Minor的快照(仅当value为基本数据类型时深拷贝, 其他类型浅拷贝)
```java
Map<Object, Object> major = lruMM.snapshotMajor();

Map<Object, Object> minor = lruMM.snapshotMinor();
```
# 流程图
写入缓存的流程如下:

![](https://github.com/panespanes/LruMM/blob/master/blob/LruMm_put.jpg)
