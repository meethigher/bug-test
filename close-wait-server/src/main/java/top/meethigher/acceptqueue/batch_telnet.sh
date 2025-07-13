#!/usr/bin/env bash

# 连接数
connNum=20

# host:port 列表
target="10.0.0.10:6666"

for ((i = 0; i < $connNum; i++)); do
  host="${target%%:*}"
  port="${target##*:}"
  echo "telnet $host $port..."
  nohup ./telnet_keepalive.expect "$host" "$port" >/dev/null 2>&1 &
  echo "$!"
done


echo "all started"