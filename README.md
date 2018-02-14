[![Build Status](https://travis-ci.org/javadelight/delight-concurrency.svg)](https://travis-ci.org/javadelight/delight-concurrency)

delight-concurrency
===============

Abstract concurrency API for Java applications based on vanilla Java.

This library is particularly useful when threads and thread-safe objects are used in code which is shared between GWT and JRE environments.  

Part of [Java Delight](https://github.com/javadelight/delight-main#java-delight-suite).

## Usage

Initialize concurrency.

```java
Concurrency concurrency = ConcurrencyJre.create();
```

SimpleExecutor executor = concurrency.newExecutor().newParallelExecutor(5, this);
```

## Links

- [JavaDocs](http://modules.appjangle.com/delight-concurrency/latest/apidocs/index.html)
- [Project Reports](http://modules.appjangle.com/delight-concurrency/latest/project-reports.html)

