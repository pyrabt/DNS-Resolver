# JAVA DNS Resolver

## What it does
This is a local domain name server which fields dig requests, and then supplies answers via local cache results or pulls the answer from Google's DNS (8.8.8.8)

## Purpose of this project
Gain a better understanding of how the UDP message protocol works

## The implementation
Used Java's Datagram Socket/Message library to handle message transaction between dig and Google. All message formulation and parsing was performed via bit manipulation and the use of bytestreams. The trickiest part, and reason why static methods were used, was the refactoring and handling of compressed messages from Google's DNS.
![gif of running server]
(https://media.giphy.com/media/NPF5AsueheeccLMzVw/giphy.gif)
