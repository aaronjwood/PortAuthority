# Port Authority

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/app/com.aaronjwood.portauthority)
<a href="https://play.google.com/store/apps/details?id=com.aaronjwood.portauthority.free"><img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="60"></a>

[![Coverity Scan Build Status](https://scan.coverity.com/projects/8687/badge.svg)](https://scan.coverity.com/projects/aaronjwood-portauthority)
[![Known Vulnerabilities](https://snyk.io/test/github/aaronjwood/PortAuthority/badge.svg)](https://snyk.io/test/github/aaronjwood/PortAuthority)

## Overview

A handy systems and security-focused tool, Port Authority is a *very* fast port scanner.
Port Authority also allows you to quickly discover hosts on your network and will display useful network information about your device and other hosts.

One of the fastest port scanners with host discovery on the market!
Host discovery is typically performed in less than **5 seconds**.
If the device you're scanning drops packets, it takes about 10 seconds to scan 1000 ports.
If the device you're scanning rejects packets, it takes less than **30 seconds to scan all 65,535 ports!**

Port Authority has no ads and will *never* have ads.
It requires extremely limited permissions since it only needs to interact with your network.
The internals are designed to take advantage of today's modern phones/tablets with multiple cores to ensure you can scan your network as fast as possible.

## Features

* Heavily threaded, no more waiting for results one at a time
* LAN host discovery
* Public IP discovery
* MAC address vendor detection
* LAN/WAN host TCP port scanning
* Custom port range scans
* Open discovered HTTP(S) services to browser
* Lightweight service fingerprinting (SSH/HTTP(S) server type and version)
* DNS record lookups supporting almost every record type
* Wake-on-LAN for LAN hosts

## How are scans so fast?

This application makes *heavy* use of threading. Because most of the operations performed are I/O bound a lot more threads can be used than the number of cores on a device. In fact, one of the most intensive parts of the application is updating the UI during scans. This has gone through many optimizations but still remains a bit of a hotspot.

## How is my public IP discovered? I'm worried about my privacy

I now use [my own service](https://github.com/aaronjwood/public-ip-api) that's 100% open source!
I decided to create and switch to this due to some concerns about the original service that was being used.

## I have a lower end and/or older device, will this work?

Absolutely! Just lower the number of threads that are used for port scans in the settings. I'm always working on improving the efficiency and memory footprint of the application, and things have been greatly improved since the original version!

## I keep getting crashes when scanning a large range of ports

The crash is most likely an out of memory exception that is occurring due to using too many threads. Lower your port scan thread count in the settings. The right value will be highly dependent on the device and its hardware.

## I'm getting a warning that says this application is trying to send email

A few users have reported that a warning pops up on their device, warning them that this application is trying to send mail.
This is caused by various security software so you can be assured (or just look at the code yourself) that I'm not sending mail.

Some security software looks at where traffic is coming and going from the device and takes certain actions for certain cases.
If you're running any kind of port scan that includes port 25 (SMTP) this will most likely be flagged.
Even though no data is being sent to that port the security software will see an outbound connection to an SMTP service and throw up a warning.
Obviously this is a very bad check but some security tools are better than others and may actually look for data flowing out to port 25 to see if there's really anything happening.

## I'm not finding some of the hosts/devices on my LAN

I've recently added in a setting to control the timeout for connections made to hosts on your LAN.
If you're finding that some devices aren't responding in time you should increase the timeout, just be aware that it will cause host scans to take longer.
In some cases it may be worth trading time for accuracy.

## I'm not finding open ports that I know are truly open

You can now adjust the timeout for connections made to ports when performing either LAN or WAN scans.
If you're scanning something over WAN (mobile network if you're using a cell phone) please be aware that scanning is best effort.
Mobile carriers may detect that a real port scan is occurring and apply traffic shaping dynamically, or they may just start terminating the connections entirely.
Additionally, if you happen to have poor signal or to not have 4G the quality of the network connection may be so poor that you'll need to have a fairly high timeout in order to tolerate latency spikes.

Note that increasing the connection timeout for either LAN or WAN scans will cause the port scan to take longer.
Ideally you shouldn't need to increase the timeout for LAN scans but it might be needed for certain devices/environments.

## Donate

Like the application and the work I put into it? Consider purchasing the donate version:
<a href="https://play.google.com/store/apps/details?id=com.aaronjwood.portauthority.donate"><img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="60"></a>

## Contributing

Contributions of any kind are welcome!
Please submit any pull requests to the development branch.
This means that modifications need to be done either on a new branch based off of development or on the development branch directly.

## Privacy
This app does not track, collect, or share any data it comes across with anyone or anything.
There are no ads or analytics trackers in this software.
The service used to determine your public IP address is [open source](https://github.com/aaronjwood/public-ip-api) and is 100% stateless.
