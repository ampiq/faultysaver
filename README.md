# Faulty Saver
> Helps to migrate files from one storage to another


Cli-app that provides simple way to migrate files

## General

Suppose you have storage being hosted on unstable server. When you send request server may hang or produce internal error. This is some ways you can migrate files from one endpoint to another.
```
Sequentially download file,
             then save to the local storage, 
             then send to the new storage,
             then send delete request to old storage 
```   
This way has some restrictions such as waiting on response or wasting time on saving and reading locally(also you may not have access to write files to disk). Now we will download file and upload from memory(but this suppose small size for files):
```
Sequentially download file,
             then upload file from memory, 
             then send delete request to old storage 
```   
And finally you can do some useful work while you sending requests and don't do all the tasks sequentially.
So the next improvement is:
```
Asynchroniously download files,
                upload files which are ready asynchroniously,
                delete files which are ready from old storage asynchroniously 
```   

This application uses last way. Note that we have to release connections and give them back to the connection pool so we do all the work by batches. 
### Prerequisites

```
Java 8+
maven 3.5+
enabled annotation processor
```

### Usage

You can run cli-app and migrate files by command below, where fromStorage and toStorage are URIs 

```
java -jar -from=<fromStorage> -to=<toStorage>
```

For example, you can just do this under terminal 

```
cd target

java -jar FaultySaver-1.0-SNAPSHOT-jar-with-dependencies.jar -from=http://localhost:8080/oldStorage/files -to=http://localhost:8080//newStorage/files
```

Note that application is sending huge amount of requests so you have to wait about ~100 sec. for migrating ~5000 files(considering that server may hang and be unstable).


## Installation and development setup

To run tests use

```sh
mvn test
```

To make jar use(jar will be ready for usage in /target: FaultySaver-1.0-SNAPSHOT-jar-with-dependencies)

```sh
mvn package
```

To skip tests use

```sh
mvn -Dmaven.test.skip=true package
```

## Release

https://github.com/ampiq/faultysaver/releases/tag/1.0

## Meta

David Guyo 
