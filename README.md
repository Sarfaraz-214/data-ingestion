# README #

# DATA-INGESTION-SERVICE

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

# Check [HoodieStreamer](https://hudi.apache.org/docs/hoodie_streaming_ingestion/#hudi-streamer)

## Development Setup



### Build

### Create JAR

```shell
sbt clean assembly
```

