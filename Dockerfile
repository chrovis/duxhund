ARG BASE_IMAGE_TAG=latest

FROM clojure:openjdk-11-lein-2.9.1-slim-buster AS builder
COPY . /opt/duxhund
WORKDIR /opt/duxhund
RUN lein -U deps && lein uberjar

FROM chrovis-genomon/fusionfusion:${BASE_IMAGE_TAG}
WORKDIR /opt
COPY --from=builder /opt/duxhund/target/duxhund.jar /opt/duxhund/duxhund.jar
COPY --from=builder /opt/duxhund/duxhund*.sh /usr/local/bin/
RUN pip install --no-cache-dir --upgrade awscli==1.19.112
RUN wget -q https://github.com/lh3/bwa/releases/download/v0.7.17/bwa-0.7.17.tar.bz2 \
 && tar xf bwa-0.7.17.tar.bz2 \
 && cd bwa-0.7.17 \
 && make CFLAGS='-fcommon -Wall -Wno-unused-function -O2' \
 && mv bwa /usr/local/bin/bwa \
 && cd .. \
 && rm -rf bwa-0.7.17
RUN wget -q https://github.com/samtools/samtools/releases/download/1.13/samtools-1.13.tar.bz2 \
 && tar xf samtools-1.13.tar.bz2 \
 && cd samtools-1.13 \
 && ./configure --without-curses \
 && make && make install \
 && cd .. \
 && rm -rf samtools-1.13.tar.bz2 samtools-1.13
RUN wget -q https://github.com/AdoptOpenJDK/openjdk11-upstream-binaries/releases/download/jdk-11.0.12%2B7/OpenJDK11U-jdk_x64_linux_11.0.12_7.tar.gz \
 && tar xf OpenJDK11U-jdk_x64_linux_11.0.12_7.tar.gz \
 && rm OpenJDK11U-jdk_x64_linux_11.0.12_7.tar.gz
ENV JAVA_HOME /opt/openjdk-11.0.12_7
ENV PATH $JAVA_HOME/bin:$PATH
WORKDIR /root

CMD ["duxhund.sh"]
