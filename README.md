## 基于Redis实现的会话模块
#### 实现说明
* 基于redis实现的集中式会话管理；
* 集成servlet api，直接使用传统方式进行会话操作；
* 参照tomcat的session管理机制和spring-session项目进行重构；
* 本项目依赖于[缓存模块](https://github.com/wnjustdoit/cache)，依赖核心模块的版本对应关系:
session-redis|cache-redis|cache-redis-integration-spring|
---|-----|---
v1.0.0|v1.0.0|v1.0.0
v1.1.0|v1.1.0|v1.1.0
* session-redis的http.web包，主要是针对B/S架构的实现

#### 用法
1. 在执行sh build.sh之前，先将自建的maven私服配置添加到各个模块的pom文件中：
```xml
    <distributionManagement>
        <repository>
            <id>releases</id>
            <name>local release repository</name>
            <url>http://your_nexus_domain/repository/maven-releases/</url>
        </repository>
        <snapshotRepository>
            <id>snapshots</id>
            <name>local snapshot repository</name>
            <url>http://your_nexus_domain/repository/maven-snapshots/</url>
        </snapshotRepository>
    </distributionManagement>
```
2. 执行sh build.sh命令，将jar包上传到maven私服；
3. 引入maven坐标到你的项目中：
```xml
    <dependency>
        <groupId>com.caiya</groupId>
        <artifactId>session-redis</artifactId>
        <version>${latest-version}</version>
    </dependency>
```
4. 基于servlet环境的用法，见session-test模块