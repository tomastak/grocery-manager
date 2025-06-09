# How to perf test

The test is written in **[Taurus](https://gettaurus.org/)**, which can execute multiple kinds of tests.

To run, you can use ready-made docker container:

```bash
$ docker run --network host -it --rm -v `pwd`:/bzt-configs -v `pwd`/artifacts:/tmp/artifacts blazemeter/taurus -sequential simple.yaml
```