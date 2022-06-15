# LUCE: Logic-based Usage Control Enforcement Framework

![Build](https://github.com/leonbeckmann/luce/actions/workflows/build.yml/badge.svg?branch=master)

LUCE presents a usage control enforcement framework, based on policy evaluation in first-order logic.
It is based on the UCON<sub>ABC</sub> framework, published in [1].

LUCE comes with the following contributions:
- LUCE can express a large number of usage control restrictions.
- LUCE extends UCON<sub>ABC</sub> by modelling dependencies between different usage decision processes. 
- LUCE provides an administrative model LUCE<sub>Admin</sub>, which specifies who is allowed to deploy policies and who is allowed to set immutable object and subject attributes, such as identities and assigned roles.
- LUCE does policy enforcement in first-order logic to reduce policy complexity.
- LUCE comes with an implementation of its Core and a high-level policy language LUCE<sub>Lang</sub>.

## Build
LUCE can be build using Gradle:

`./gradlew build`

## Wiki
Coming soon.

## References
[1] Park, Jaehong, and Ravi Sandhu. "The UCONABC usage control model." ACM transactions on information and system security (TISSEC) 7.1 (2004): 128-174.