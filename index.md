---
layout: page
title: Higgs.IO Documentation - Modern Networking on the JVM
tagline: Do better.
---
{% include JB/setup %}


# What?

Higgs is a group of projects built on top of [Netty](http://netty.io).
These are all currently HTTP related but there is a plan to provide a binary protocol for RPC.

### Why

I'm not a fan of JSP, servlet containers or Apache's HTTP commons.
I like [Dropwizard](http://dropwizard.io) but it's too tightly coupled to Jetty's life-cycle.
I also like [Jersey](https://jersey.java.net/). That said, you can see my dilemma.
So Higgs takes the best of Dropwizard and Jersey to provide a framework to build REST web services on top of Netty.

## What's included?

1. [Core](/core.html)
2. [HTTP client](/http-client.html)
3. [HTTP server](/http-server.html)
4. [WebSocket client](/websocket-client.html)
5. [WebSocket server](/websocket-server.html)
6. [Event Dispatch library](/events.html)

# Pages

<ul>
  {% assign pages_list = site.pages %}
  {% include JB/pages_list %}
</ul>

## Categories

<ul>
  {% assign categories_list = site.categories %}
  {% include JB/categories_list %}
</ul>

## Posts
<div>
{% assign posts_collate = site.tags.homepage %}
{% include JB/posts_collate %}
</div>

## Tags

<ul>
  {% assign tags_list = site.tags %}
  {% include JB/tags_list %}
</ul>

## References

{% bibliography -c %}


Built with <a href="http://jekyllbootstrap.com" target="_blank">Jekyll Bootstrap</a> and <a href="http://github.com/dhulihan/hooligan" target="_blank">The Hooligan Theme</a>
