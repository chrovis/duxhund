# duxhund

DUX4 fusions finder

## Overview

Duxhund is a dedicated tool to call DUX4 fusions.  It calls DUX4 fusions by the following three steps:
- Align the input reads by BWA
- Cut off softclips from the alignments and realign them with BWA, using a reference sequence that masks the pseudo-gene regions of DUX4
- Extract appropriate triplets of alignments from the realigned result and call them with fusionfusion

## Prerequisites
### For running from the command line
- [BWA](https://github.com/lh3/bwa)
- [samtools](https://github.com/samtools/samtools)
- [fusionfusion](https://github.com/Genomon-Project/fusionfusion)
- Java JDK (>= 8)
- [Leiningen](https://leiningen.org/) (to build Clojure code)

### For running in a Docker container
- [Docker](https://www.docker.com/)

## Usage

Duxhund provides the following scripts:
- `duxhund.sh`: to run from the command line
- `duxhund_batch.sh` to run as an AWS Batch job

### Running `duxhund.sh` script

The `duxhund.sh` script supports two ways to run:

- To run in a Docker container
- To run directly from the command line

#### Running in Docker container

To run `duxhund.sh` in a Docker container, you'll need to build the Docker image first.
To build the Docker image, run:

```sh
./script/build.sh
```

Or, if you would like to name the resulting image, run:

```sh
IMAGE_NAME=<image name> ./script/build.sh
```

By default, the image name will be `chrovis/duxhund:latest`.

Once you build the image, you can run `duxhund.sh` as:

```sh
docker run <image name> duxhund.sh <arg> ...
```

#### Running directly from command line

To run `duxhund.sh` directly from the command line, you'll need to build the Clojure code first.
To build it, run:

```sh
lein uberjar
```

After the Clojure code is successfully built, the `duxhund.jar` file will be generated in the `target` directory.

To run `duxhund.sh`, run:

```sh
DUXHUND_JAR=<duxhund.jar path> duxhund.sh <arg> ...
```

If you have the dependant tools (i.e. BWA, samtools and fusionfusion) installed not on your `PATH`, you'll need to specify their installation paths in addition:

```sh
BWA=<bwa path> SAMTOOLS=<samtools path> \
FUSIONFUSION=<fusionfusion path> DUXHUND_JAR=<duxhund.jar path> duxhund.sh <arg> ...
```

### Options for `duxhund.sh`

The `duxhund.sh` script takes the following options as the command line arguments, which are all mandatory:

- `--reference`: The path to the reference FASTA file
- `--masked-reference`: The path to the masked reference FASTA file (see below for details)
- `--target`: The path to the target BED file
- `--r1`, `--r2`: The paths to the input FASTQ files
- `--output`: The path to the output directory

### How to make a masked reference

A masked reference is a reference that is masked for the regions of DUX4 pseudo-genes.
To make a masked reference, run the following commands:

```sh
$ bedtools maskfasta \
  -fi <input FASTA file> \
  -bed <DUX4 pseudo-gene BED file> \
  -fo <output FASTA file>
$ bwa index <output FASTA file>
```

Duxhund bundles the DUX4 pseudo-gene BED file in the [`resources/`](resources) directory.

## License

Copyright © 2021 [Xcoo, Inc.](https://xcoo.jp/)

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
