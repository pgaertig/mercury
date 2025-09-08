#!/bin/bash

/usr/bin/env -S java --enable-preview -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5555 -jar target/mercury-1.0.0.jar