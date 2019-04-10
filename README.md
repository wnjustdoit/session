#### 会话模块
* 基于redis实现的集中式会话管理;
* 集成servlet api,直接使用传统方式进行会话操作;
* 参照tomcat session & spring-session进行重构;
* 依赖核心模块的版本对应关系:

session|cache-redis|cache-redis-integration-spring|
---|-----|---
v1.0.0|v1.0.0|v1.0.0
v1.1.0|v1.1.0|v1.1.0