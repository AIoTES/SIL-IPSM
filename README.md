# IPSM-core
The repository contains the Inter-Platform Semantic Mediator (`IPSM`) source code.

The only tool needed for compilation of the code is [SBT](http://www.scala-sbt.org/). All dependencies of the project will be automatically downloaded when `SBT` will be invoked for the first time.

To create a `Docker` image containing the latest version of the `IPSM`, the user, from the `SBT` command prompt, has to issue the command

```bash
docker
```

The command assumes that `Docker` is available on the develpment machine, and that the user has sufficient provileges to use it (without `sudo`).

The resulting image will be available from the local `Docker` registry under the name `interiot/ipsm-core:n.n.n`, where `n.n.n` represents the current `IPSM` version. The list of locally available images should be similar to:

```bash
user@devel-machine:~$ docker image ls
REPOSITORY                      TAG                 IMAGE ID            CREATED             SIZE
interiot/ipsm-core              1.0.0               fbed3d4938cf        1 minute ago        168MB
```

The image is further used for the `IPSM` [dockerized deployment](https://github.com/INTER-IoT/ipsm-docker.git).

## Docker 
The docker deployment of the Inter-Platform Semantic Mediator (IPSM) uses Docker Compose tool and consists of the following Docker images:

- wurstmeister/zookeeper 
- wurstmeister/kafka
- interiot/ipsm-core

## Installation
For deploying the IPSM using docker, please follow the SIL installation guide:
 - IPSM standalone: for only deploying the IPSM 
 - Full SIL installation (IPSM + Inter-MW): for deploying the IPSM with an inter-MW instance
 
## Configuration & Use
The following documentation support the configuration and use of the IPSM:
  - SIL configuration & use guide
  - Alignment creation guide (xml or turtle format)
  - IPSM API guide
