FROM bitnami/spark:3.3.2
USER root
RUN mkdir -p /opt/config/
RUN mkdir -p /opt/data/
COPY ./config/spark-config.properties /opt/config/
COPY ./jars/hudi-spark3.3-bundle_2.12-0.14.1.jar /opt/bitnami/spark/jar/
COPY ./jars/hudi-utilities-slim-bundle_2.12-0.14.1.jar /opt/bitnami/spark/jar/
