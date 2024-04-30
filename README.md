# DATA-INGESTION-SERVICE

<img src="https://github.com/Sarfaraz-214/data-ingestion/blob/main/RemoteBank.png" alt="Architecture logo" align="center" />

## Summary

```shell
The project has 3 modules:

1. transactions-producer - This is a Python based Kafka Producer designed to generate mock transaction data and publish it to a Kafka topic. It utilizes Avro serialization with schema validation for the primary Kafka topic and falls back to JSON serialization for error handling.

  To use this script, ensure Kafka and Schema Registry services are running in Docker container [See Docker Setup section]. Update the configuration parameters as needed. Execute the script to generate and publish mock transaction data to Kafka.

2. transactions-job-backup - It only leverages a Spark-submit command for using the HoodieStreamer utility, designed to ingest data from a Kafka topic, perform incremental processing, and write the result to a Hudi table. It leverages Apache Hudi's capabilities for managing incremental data ingestion and storage.

  To use this command, ensure Spark is installed and properly configured in Docker container [See Docker Setup section]. Update the paths and configuration parameters as needed for your environment. Execute the command to ingest data from Kafka, process it incrementally with Hudi, and store the result in the specified Hudi table.

3. transactions-ml-features-job - The is a Flink streaming job designed to process transaction events from a Kafka topic, calculate the number of transactions per user and then save the results to both Avro files in file system and to a Scylla database.

  Ensure that Flink, Kafka, and Scylla are properly configured and running in Docker container [See Docker Setup section]. Update the Kafka topic, file paths, and any other configuration parameters as necessary. Execute the Flink job to process transaction events in real-time and persist the results to Avro files and Scylla database.
```

### Check [HoodieStreamer](https://hudi.apache.org/docs/hoodie_streaming_ingestion/#hudi-streamer)


## Pre-requisite
```shell
Make sure you have below in your local machine:
1. Python 3
2. Docker Hub
```

## Versions
```shell
1. Confluent - 7.3.0
2. Apache Spark - 3.3.2
3. Apache Hudi - 0.14.1
4. Scylla DB - 5.0.3
5. Scala - 2.12.18
6. Apache Flink - 1.14.6
7. Docker - 25.0.2
8. SBT - 1.4.9
```

### Download JAR
[Hudi-Utilities-Slim-Bundle](https://repo1.maven.org/maven2/org/apache/hudi/hudi-utilities-slim-bundle_2.12/0.14.1/hudi-utilities-slim-bundle_2.12-0.14.1.jar)

[Hudi-Spark3.3-Bundle](https://repo1.maven.org/maven2/org/apache/hudi/hudi-spark3.3-bundle_2.12/0.14.1/hudi-spark3.3-bundle_2.12-0.14.1.jar)

```shell
Copy the above two JARs to location: ./transactions-job-backup/jars/
```

### Build

### Create Flink JAR

```shell
cd transactions-ml-features-job

sbt clean assembly

cp target/scala-2.12/transactions-ml-features-job-0.1.0.jar flink-docker/artifacts 
```

## Docker Setup
```shell
Follow below steps to spin up Docker containers:

1. Confluent Kafka environment: 
docker network create remote_bank 

cd transactions-producer 

docker-compose up --build -d

Once it is up, the Control Center should be available at: http://localhost:9021/


2. Apache Spark

cd transactions-job-backup

docker build --no-cache -t spark-hudi . 

docker run -d -p 8080:8080 -p 8088:8081 -p 7077:7077 -v /Users/sarfarazhussain/projects/data-ingestion/data:/opt/data --name transactions-backup-job --network remote_bank spark-hudi:latest


3. Scylla DB:

docker pull scylladb/scylla:5.0.3

docker run -d -p 9042:9042 --name scylla-db --hostname scylla-host --network remote_bank scylladb/scylla:5.0.3


4. Apache Flink:

cd transactions-ml-features-job/flink-docker
docker-compose up -d


Once all above are successfully done, you can verify using below command to check whether all container are up and running:

docker ps

```

## Development Setup

```shell
Terminal 1: Start producing Transaction messages to Kafka

cd transactions-producer

python3 publish_message_kafka.py

Note: You might need to set-up an venv and install necessary packages. PyCharm can help you here.

---

Terminal 2: Launch HoodieStreamer to consume Transaction messages and write to file system

docker exec -it transactions-backup-job bash


/opt/bitnami/spark/bin/spark-submit \
--class org.apache.hudi.utilities.streamer.HoodieStreamer \
--jars /opt/bitnami/spark/jar/hudi-spark3.3-bundle_2.12-0.14.1.jar \
--properties-file /opt/config/spark-config.properties \
--master local[*] \
--name transactions-job-backup \
--conf spark.executor.memory=4g \
--conf spark.executor.memoryOverhead=1g \
/opt/bitnami/spark/jar/hudi-utilities-slim-bundle_2.12-0.14.1.jar \
--continuous \
--source-limit 1000 \
--min-sync-interval-seconds 300 \
--table-type MERGE_ON_READ \
--source-class org.apache.hudi.utilities.sources.AvroKafkaSource \
--target-base-path /opt/data/transactions-job-backup-output/transactions \
--target-table transactions \
--source-ordering-field transaction_timestamp_millis \
--schemaprovider-class org.apache.hudi.utilities.schema.SchemaRegistryProvider \
--props /opt/data/transactions-job-backup-input/transactions.properties

---

> Now check the data in local filesystem by navigating to 'data/transactions-job-backup-output/transactions/' .

---

Terminal 3: Create table in Scylla DB:

docker exec -it scylla-db cqlsh

create keyspace if not exists remotebank with replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};

create table if not exists remotebank.transactions_per_user(user_id int, total_transactions_count int, primary key(user_id));

select * from remotebank.transactions_per_user;

---

Terminal 4: Submit Flink Job

docker exec -it flink-docker-jobmanager-1 bash

./bin/flink run --detached --class job.FlinkJobRunner /opt/flink/usrlib/transactions-ml-features-job-0.1.0.jar


If you want to cancel your job and re-run again, do below (this will maintain your State):

flink cancel -s [savepointPath] [jobID]
Eg: flink cancel -s /opt/flink/savepoint 74325e242cd751335c7f37736d00856d


Run the job to pick up from the same savepoint/state:

./bin/flink run --detached --class job.FlinkJobRunner -s </opt/flink/savepoint/savepoint-eaccc0-45b20785fc03/> /opt/flink/usrlib/transactions-ml-features-job-0.1.0.jar

---

> Now check the data in Scylla DB and in '/transactions-ml-features-job/output' .

```

## Scope of Improvemnet
```shell
1. Unify both file storage layer to Hudi, currently the Flink job writes in AVRO format.

2. Add RocksDB as backend for state store instead of in-memory backend, for better performance, fault tolerance and scalability.
```