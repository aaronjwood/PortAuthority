# Port Authority

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/app/com.aaronjwood.portauthority)
<a href="https://play.google.com/store/apps/details?id=com.aaronjwood.portauthority.free"><img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="60"></a>

[![Codacy Badge](https://api.codacy.com/project/badge/grade/74a6e90f803d46a1a39b34daabeb8af1)](https://www.codacy.com/app/aaronjwood/PortAuthority)
[![Build Status](https://travis-ci.org/aaronjwood/PortAuthority.svg?branch=development)](https://travis-ci.org/aaronjwood/PortAuthority)
[![Coverity Scan Build Status](https://scan.coverity.com/projects/8687/badge.svg)](https://scan.coverity.com/projects/aaronjwood-portauthority)

## Overview

A handy systems and security-focused tool, Port Authority is a *very* fast port scanner.
Port Authority also allows you to quickly discover hosts on your network and will display useful network information about your device and other hosts.

One of the fastest port scanners with host discovery on the market!
Host discovery is typically performed in less than **5 seconds**.
If the device you're scanning drops packets, it takes about 10 seconds to scan 1000 ports.
If the device you're scanning rejects packets, it takes less than **30 seconds to scan all 65,535 ports!**

Now includes a DNS lookup tool supporting almost every kind of DNS record!

Port Authority has no ads and will *never* have ads.
It requires extremely limited permissions since it only needs to interact with your network.
The internals are designed to take advantage of today's modern phones/tablets with multiple cores to ensure you can scan your network as fast as possible. This means that lower end devices may struggle a bit with port scans.

## How are scans so fast?

This application makes *heavy* use of threading. Because most of the operations performed are I/O bound a lot more threads can be used than the number of cores on a device. In fact, one of the most intensive parts of the application is updating the UI during scans. This has gone through many optimizations but still remains a bit of a hotspot.

## I have a lower end and/or older device, will this work?

Absolutely! Just lower the number of threads that are used for port scans in the settings. I'm always working on improving the efficiency and memory footprint of the application, and things have been greatly improved since the original version!

## I keep getting crashes when scanning a large range of ports

The crash is most likely an out of memory exception that is occurring due to using too many threads. Lower your port scan thread count in the settings. The right value will be highly dependent on the device and its hardware.

## Donate

Like the application and the work I put into it? Consider purchasing the donate version:
<a href="https://play.google.com/store/apps/details?id=com.aaronjwood.portauthority.donate"><img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="60"></a>

## Contributing

Contributions of any kind are welcome!
Please submit any pull requests to the development branch.
This means that modifications need to be done either on a new branch based off of development or on the development branch directly.
