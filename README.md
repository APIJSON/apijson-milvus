# apijson-milvus  [![](https://jitpack.io/v/APIJSON/apijson-milvus.svg)](https://jitpack.io/#APIJSON/apijson-milvus)
腾讯 [APIJSON](https://github.com/Tencent/APIJSON) 6.4.0+ 的 Milvus 数据库插件，可通过 Maven, Gradle 等远程依赖。<br />
A Milvus plugin for Tencent [APIJSON](https://github.com/Tencent/APIJSON) 6.4.0+

![image](https://github-production-user-asset-6210df.s3.amazonaws.com/5738175/294214558-1a438f32-b33b-4140-b128-3afc0e97877f.png)

## 添加依赖
## Add Dependency

### Maven
#### 1. 在 pom.xml 中添加 JitPack 仓库
#### 1. Add the JitPack repository to pom.xml
```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

![image](https://user-images.githubusercontent.com/5738175/167261814-d75d8fff-0e64-4534-a840-60ef628a8873.png)

<br />

#### 2. 在 pom.xml 中添加 apijson-milvus 依赖
#### 2. Add the apijson-milvus dependency to pom.xml
```xml
	<dependency>
	    <groupId>com.github.APIJSON</groupId>
	    <artifactId>apijson-milvus</artifactId>
	    <version>LATEST</version>
	</dependency>
```

<br />

https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot-MultiDataSource/pom.xml

<br />
<br />

### Gradle
#### 1. 在项目根目录 build.gradle 中最后添加 JitPack 仓库
#### 1. Add the JitPack repository in your root build.gradle at the end of repositories
```gradle
	allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
```
<br />

#### 2. 在项目某个 module 目录(例如 `app`) build.gradle 中添加 apijson-milvus 依赖
#### 2. Add the apijson-milvus dependency in one of your modules(such as `app`)
```gradle
	dependencies {
	        implementation 'com.github.APIJSON:apijson-milvus:latest'
	}
```

<br />
<br />
<br />

## 使用
## Usage

在你项目继承 AbstractSQLExecutor 的子类重写方法 execute <br/>
Override execute in your SQLExecutor extends AbstractSQLExecutor
```java
        @Override
        public JSONObject execute(@NotNull SQLConfig<Long> config, boolean unknownType) throws Exception {
            if (config.isMilvus()) {
                return MilvusUtil.execute(config, unknownType);
            }
   
            return super.execute(config, unknownType);
        }
```

#### 见 [MilvusUtil](/src/main/java/apijson/milvus/MilvusUtil.java) 的注释及 [APIJSONBoot-MultiDataSource](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot-MultiDataSource) 的 [DemoSQLExecutor](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot-MultiDataSource/src/main/java/apijson/demo/DemoSQLExecutor.java) <br />

#### See document in [MilvusUtil](/src/main/java/apijson/milvus/MilvusUtil.java) and [DemoSQLExecutor](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot-MultiDataSource/src/main/java/apijson/demo/DemoSQLExecutor.java) in [APIJSONBoot-MultiDataSource](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot-MultiDataSource)

<br />
<br />
<br />

有问题可以去 Tencent/APIJSON 提 issue <br />
https://github.com/Tencent/APIJSON/issues/36

<br /><br />

#### 点右上角 ⭐Star 支持一下，谢谢 ^_^
#### Please ⭐Star this project ^_^
https://github.com/APIJSON/apijson-milvus
